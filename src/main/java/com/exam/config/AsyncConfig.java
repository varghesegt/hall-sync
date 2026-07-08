package com.exam.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated bounded thread pool for async allocation.
 *
 * Uses Spring's ThreadPoolTaskExecutor for:
 * - Graceful shutdown lifecycle management
 * - Queue capacity control with explicit rejection policy
 * - Named threads for debugging
 * - MDC propagation via TaskDecorator
 * - Micrometer metrics for submitted/running/completed/failed jobs
 */
@Configuration
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "allocationExecutor")
    public ThreadPoolTaskExecutor allocationExecutor(MeterRegistry meterRegistry) {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(5);
        exec.setMaxPoolSize(5);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("alloc-");
        exec.setTaskDecorator(metricsAndMdcDecorator(meterRegistry));
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);

        // Fix #4: Explicit rejection policy — AbortPolicy throws RejectedExecutionException
        // which is caught by GlobalExceptionHandler and translated to 503
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        exec.initialize();
        return exec;
    }

    /**
     * Combines MDC propagation, TenantContext propagation, and job lifecycle metrics.
     * CRITICAL: Without TenantContext propagation, async allocation jobs would
     * fall back to the MASTER database and write student data into the wrong DB.
     */
    private TaskDecorator metricsAndMdcDecorator(MeterRegistry meterRegistry) {
        return runnable -> {
            // ── Capture parent thread context ──
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            meterRegistry.counter("allocation.jobs.submitted").increment();

            return () -> {
                // ── Restore context in child thread ──
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                if (tenantId != null) {
                    com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                }
                meterRegistry.counter("allocation.jobs.running").increment();
                try {
                    runnable.run();
                    meterRegistry.counter("allocation.jobs.completed").increment();
                } catch (Exception e) {
                    meterRegistry.counter("allocation.jobs.failed").increment();
                    throw e;
                } finally {
                    com.exam.config.tenant.TenantContext.clear();
                    MDC.clear();
                }
            };
        };
    }
}
