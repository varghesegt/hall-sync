package com.exam.config.tenant;

import com.exam.entity.master.Tenant;
import com.exam.repository.master.TenantRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties masterDataSourceProperties() {
        return new DataSourceProperties();
    }

    public static void normalizeDataSourceProperties(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        url = url.trim();

        String work = url;
        if (work.startsWith("jdbc:")) {
            work = work.substring(5);
        }

        if (work.startsWith("postgres://")) {
            work = "postgresql://" + work.substring("postgres://".length());
        }

        // Check if credentials are embedded: postgresql://user:password@host:port/dbname...
        int slashSlash = work.indexOf("://");
        int atIndex = work.lastIndexOf("@");
        if (slashSlash != -1 && atIndex != -1 && atIndex > slashSlash) {
            String userInfo = work.substring(slashSlash + 3, atIndex);
            String remainder = work.substring(atIndex + 1);
            int colonIndex = userInfo.indexOf(":");
            if (colonIndex != -1) {
                String extractedUser = userInfo.substring(0, colonIndex);
                String extractedPass = userInfo.substring(colonIndex + 1);
                if (properties.getUsername() == null || properties.getUsername().trim().isEmpty() || "postgres".equals(properties.getUsername())) {
                    properties.setUsername(extractedUser);
                }
                if (properties.getPassword() == null || properties.getPassword().trim().isEmpty() || "jittu007".equals(properties.getPassword())) {
                    properties.setPassword(extractedPass);
                }
            }
            work = "postgresql://" + remainder;
        }

        String finalJdbcUrl = work.startsWith("postgresql:") ? "jdbc:" + work : url;
        properties.setUrl(finalJdbcUrl);
        if (properties.getDriverClassName() == null || properties.getDriverClassName().trim().isEmpty()) {
            properties.setDriverClassName("org.postgresql.Driver");
        }
        logger.info("Initialized Master Database URL: {}", maskUrl(finalJdbcUrl));
    }

    private static String maskUrl(String url) {
        if (url == null) return "";
        return url.replaceAll("(?<=://)[^/@]+@", "***:***@");
    }

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource(DataSourceProperties masterDataSourceProperties) {
        normalizeDataSourceProperties(masterDataSourceProperties);
        return masterDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public TenantRoutingDataSource dataSource(@Qualifier("masterDataSource") DataSource masterDataSource,
                                              DataSourceProperties masterDataSourceProperties) {
        normalizeDataSourceProperties(masterDataSourceProperties);
        TenantRoutingDataSource customDataSource = new TenantRoutingDataSource();
        customDataSource.setMasterDataSource(masterDataSource);
        customDataSource.setMasterProperties(masterDataSourceProperties);
        
        Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
        targetDataSources.put("MASTER", masterDataSource);
        
        customDataSource.setTargetDataSources(targetDataSources);
        customDataSource.setDefaultTargetDataSource(masterDataSource);
        
        return customDataSource;
    }
}
