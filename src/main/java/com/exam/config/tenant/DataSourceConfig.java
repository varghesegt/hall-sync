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

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        return masterDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public TenantRoutingDataSource dataSource(@Qualifier("masterDataSource") DataSource masterDataSource) {
        TenantRoutingDataSource customDataSource = new TenantRoutingDataSource();
        customDataSource.setMasterDataSource(masterDataSource);
        customDataSource.setMasterProperties(masterDataSourceProperties());
        
        Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
        targetDataSources.put("MASTER", masterDataSource);
        
        customDataSource.setTargetDataSources(targetDataSources);
        customDataSource.setDefaultTargetDataSource(masterDataSource);
        
        return customDataSource;
    }
}
