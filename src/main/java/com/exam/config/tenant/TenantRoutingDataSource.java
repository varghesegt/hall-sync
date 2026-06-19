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

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger logger = LoggerFactory.getLogger(TenantRoutingDataSource.class);

    private DataSource masterDataSource;
    private DataSourceProperties masterProperties;
    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

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

        logger.info("Initializing DataSource for tenant: {} (DB: {})", tenantId, dbName);

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

        HikariDataSource tenantDataSource = new HikariDataSource();
        tenantDataSource.setJdbcUrl(tenantUrl);
        tenantDataSource.setUsername(masterProperties.getUsername());
        tenantDataSource.setPassword(masterProperties.getPassword());
        tenantDataSource.setDriverClassName(masterProperties.getDriverClassName());
        
        targetDataSources.put(tenantId, tenantDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet(); // Re-initialize the routing data source
    }
}

