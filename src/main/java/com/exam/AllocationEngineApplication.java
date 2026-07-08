package com.exam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.exam")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = {"com.exam.repository", "com.exam.claims.repository"})
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {"com.exam.entity", "com.exam.claims.entity"})
@EnableScheduling
@org.springframework.cache.annotation.EnableCaching
public class AllocationEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllocationEngineApplication.class, args);
    }
}
