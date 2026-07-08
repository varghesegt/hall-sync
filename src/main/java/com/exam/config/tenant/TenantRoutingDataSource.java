package com.exam.config.tenant;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Production-grade multi-tenant DataSource router.
 *
 * Each tenant gets its own HikariCP connection pool with strict limits:
 * - Max 3 connections per tenant (50 tenants × 3 = 150 total, within PostgreSQL limits)
 * - Idle connections closed after 2 minutes
 * - Connections recycled every 5 minutes
 * - Pools for inactive tenants evicted after 30 minutes
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger logger = LoggerFactory.getLogger(TenantRoutingDataSource.class);

    // ── Pool Tuning Constants ──────────────────────────────────────────────
    private static final int TENANT_POOL_MAX_SIZE = 3;
    private static final int TENANT_POOL_MIN_IDLE = 1;
    private static final long TENANT_POOL_IDLE_TIMEOUT_MS = 120_000;      // 2 minutes
    private static final long TENANT_POOL_MAX_LIFETIME_MS = 300_000;      // 5 minutes
    private static final long TENANT_POOL_CONNECTION_TIMEOUT_MS = 5_000;  // 5 seconds (fail fast)
    private static final long POOL_EVICTION_INTERVAL_MS = 300_000;        // Check every 5 minutes
    private static final long POOL_EVICTION_IDLE_THRESHOLD_MS = 1_800_000; // Evict after 30 min idle

    private DataSource masterDataSource;
    private DataSourceProperties masterProperties;
    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    private final ScheduledExecutorService evictionScheduler = Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "tenant-pool-evictor"); t.setDaemon(true); return t; }
    );

    public TenantRoutingDataSource() {
        // Schedule idle pool eviction to prevent memory bloat from inactive tenants
        evictionScheduler.scheduleAtFixedRate(this::evictIdlePools,
                POOL_EVICTION_INTERVAL_MS, POOL_EVICTION_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void setMasterDataSource(DataSource masterDataSource) {
        this.masterDataSource = masterDataSource;
    }

    public void setMasterProperties(DataSourceProperties masterProperties) {
        this.masterProperties = masterProperties;
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources.putAll(targetDataSources);
        super.setTargetDataSources(this.targetDataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return "MASTER";
        }

        // Track last access time for idle pool eviction
        lastAccessTime.put(tenantId, System.currentTimeMillis());

        // If we haven't created a connection pool for this tenant yet, do it now
        if (!targetDataSources.containsKey(tenantId)) {
            createAndAddDataSource(tenantId);
        }

        return tenantId;
    }

    private synchronized void createAndAddDataSource(String tenantId) {
        // Double check lock
        if (targetDataSources.containsKey(tenantId)) return;

        // DB Name convention from TenantProvisioningService: "tenant_" + tenantId
        String dbName = "tenant_" + tenantId.toLowerCase().replaceAll("[^a-z0-9]", "");
        
        String masterUrl = masterProperties.getUrl();
        String tenantUrl = masterUrl.substring(0, masterUrl.lastIndexOf("/") + 1) + dbName;

        logger.info("Initializing DataSource for tenant: {} (DB: {}) [pool: max={}, minIdle={}]",
                tenantId, dbName, TENANT_POOL_MAX_SIZE, TENANT_POOL_MIN_IDLE);

        // Run Flyway migrations on the tenant DB to ensure schema is up to date
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(tenantUrl, masterProperties.getUsername(), masterProperties.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            logger.info("Successfully executed database migrations for tenant: {}", tenantId);
        } catch (Exception e) {
            logger.error("Failed to run database migrations for tenant: " + tenantId, e);
        }

        // ── Production-grade HikariCP pool configuration ──────────────────
        HikariDataSource tenantDataSource = new HikariDataSource();
        tenantDataSource.setPoolName("HikariPool-" + tenantId);
        tenantDataSource.setJdbcUrl(tenantUrl);
        tenantDataSource.setUsername(masterProperties.getUsername());
        tenantDataSource.setPassword(masterProperties.getPassword());
        tenantDataSource.setDriverClassName(masterProperties.getDriverClassName());

        // Strict pool sizing: 50 tenants × 3 = 150 connections (PostgreSQL default max = 100-200)
        tenantDataSource.setMaximumPoolSize(TENANT_POOL_MAX_SIZE);
        tenantDataSource.setMinimumIdle(TENANT_POOL_MIN_IDLE);

        // Connection lifecycle: recycle frequently to prevent stale connections
        tenantDataSource.setIdleTimeout(TENANT_POOL_IDLE_TIMEOUT_MS);
        tenantDataSource.setMaxLifetime(TENANT_POOL_MAX_LIFETIME_MS);
        tenantDataSource.setConnectionTimeout(TENANT_POOL_CONNECTION_TIMEOUT_MS);

        // Health validation: detect dead connections before they are handed out
        tenantDataSource.setConnectionTestQuery("SELECT 1");
        
        targetDataSources.put(tenantId, tenantDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet(); // Re-initialize the routing data source
    }

    /**
     * Evicts connection pools for tenants that haven't been accessed recently.
     * At 50 tenants, only ~10 may be active at any given time. Closing the other
     * 40 pools saves ~120 PostgreSQL connections and significant memory.
     */
    private void evictIdlePools() {
        long now = System.currentTimeMillis();
        int evicted = 0;

        for (Map.Entry<String, Long> entry : lastAccessTime.entrySet()) {
            String tenantId = entry.getKey();
            long lastAccess = entry.getValue();

            if ((now - lastAccess) > POOL_EVICTION_IDLE_THRESHOLD_MS) {
                Object removed = targetDataSources.remove(tenantId);
                lastAccessTime.remove(tenantId);

                if (removed instanceof HikariDataSource) {
                    try {
                        ((HikariDataSource) removed).close();
                        evicted++;
                        logger.info("Evicted idle connection pool for tenant: {} (idle for {}min)",
                                tenantId, (now - lastAccess) / 60_000);
                    } catch (Exception e) {
                        logger.warn("Error closing pool for tenant: {}", tenantId, e);
                    }
                }
            }
        }

        if (evicted > 0) {
            super.setTargetDataSources(targetDataSources);
            super.afterPropertiesSet();
            logger.info("Pool eviction complete: {} pools closed, {} active", evicted, targetDataSources.size() - 1);
        }
    }

    /**
     * Returns the count of currently active tenant pools (for monitoring).
     */
    public int getActiveTenantPoolCount() {
        return targetDataSources.size() - 1; // Subtract MASTER
    }
}

