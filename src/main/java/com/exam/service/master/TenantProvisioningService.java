package com.exam.service.master;

import com.exam.dto.admin.CreateTenantRequest;
import com.exam.entity.master.AppUser;
import com.exam.entity.master.Tenant;
import com.exam.repository.master.TenantRepository;
import com.exam.repository.master.UserRepository;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Service
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final DataSourceProperties masterProperties;

    public TenantProvisioningService(TenantRepository tenantRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     JdbcTemplate jdbcTemplate,
                                     DataSourceProperties masterProperties) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.masterProperties = masterProperties;
    }

    public void provisionTenant(CreateTenantRequest request) {
        if (tenantRepository.findByTenantId(request.getTenantId()).isPresent()) {
            throw new IllegalArgumentException("Tenant ID already exists.");
        }
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new IllegalArgumentException("Admin email already exists.");
        }

        String masterDbUrl = masterProperties.getUrl();
        String dbUsername = masterProperties.getUsername();
        String dbPassword = masterProperties.getPassword();

        String dbName = "tenant_" + request.getTenantId().toLowerCase().replaceAll("[^a-z0-9]", "");

        // Strict validation: only allow safe database names
        if (!dbName.matches("^[a-z0-9_]{3,63}$")) {
            throw new IllegalArgumentException("Invalid tenant ID: produces unsafe database name.");
        }

        // Create Database in PostgreSQL (quote identifier to prevent injection)
        try {
            jdbcTemplate.execute("CREATE DATABASE \"" + dbName + "\"");
        } catch (Exception e) {
            // It might already exist, log or handle
        }

        // Run Flyway migrations on the new tenant DB (preserve SSL & query params)
        String queryParams = "";
        int qIdx = masterDbUrl.indexOf("?");
        String baseUrl = (qIdx != -1) ? masterDbUrl.substring(0, qIdx) : masterDbUrl;
        if (qIdx != -1) {
            queryParams = masterDbUrl.substring(qIdx);
        }
        String basePart = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1);
        String tenantDbUrl = basePart + dbName + queryParams;
        
        Flyway flyway = Flyway.configure()
                .dataSource(tenantDbUrl, dbUsername, dbPassword)
                .locations("classpath:db/migration") // Or a specific tenant location
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        // Clear default seeded halls so that the new college starts with a blank database (0 halls)
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(tenantDbUrl, dbUsername, dbPassword);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM halls;");
        } catch (Exception e) {
            System.err.println("Failed to clear default halls: " + e.getMessage());
        }

        // Create Tenant record
        Tenant tenant = new Tenant();
        tenant.setTenantId(request.getTenantId());
        tenant.setCollegeName(request.getCollegeName());
        tenant.setDbName(dbName);
        tenant = tenantRepository.save(tenant);
        com.exam.config.tenant.TenantContext.registerTenantDetails(tenant.getTenantId(), tenant.getCollegeName(), tenant.getRemunerationRate());

        // Create Admin User for this tenant
        AppUser admin = new AppUser();
        admin.setEmail(request.getAdminEmail());
        admin.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        admin.setRole("ROLE_COLLEGE_ADMIN");
        admin.setTenant(tenant);
        userRepository.save(admin);
    }
}
