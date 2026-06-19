package com.exam.service.master;

import com.exam.entity.master.AppUser;
import com.exam.repository.master.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.username:hallsync@admin}")
    private String adminUsername;

    @Value("${app.security.password:12345678}")
    private String adminPassword;

    private final com.exam.repository.master.TenantRepository tenantRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.exam.repository.FacultyRepository facultyRepository;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, com.exam.repository.master.TenantRepository tenantRepository, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate, com.exam.repository.FacultyRepository facultyRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.facultyRepository = facultyRepository;
    }

    @Override
    public void run(String... args) {
        // Ensure KRCE Tenant exists
        if (!tenantRepository.findByTenantId("krce").isPresent()) {
            logger.info("Initializing Master Database with Tenant: KRCE");
            
            // Create the physical database first
            try {
                jdbcTemplate.execute("CREATE DATABASE tenant_krce");
                logger.info("Successfully created database 'tenant_krce'.");
            } catch (Exception e) {
                logger.warn("Database 'tenant_krce' might already exist or could not be created: {}", e.getMessage());
            }

            com.exam.entity.master.Tenant krce = new com.exam.entity.master.Tenant();
            krce.setTenantId("krce");
            krce.setCollegeName("K. RAMAKRISHNAN COLLEGE OF ENGINEERING");
            krce.setRemunerationRate(150.0);
            krce.setDbName("tenant_krce");
            tenantRepository.save(krce);
            logger.info("KRCE Tenant created successfully.");
        }

        // Ensure the KRCE Admin is present and password is always in sync
        {
            var existingKrce = userRepository.findByEmail("coe1@krce.ac.in");
            if (existingKrce.isPresent()) {
                AppUser krceAdmin = existingKrce.get();
                String freshHash = passwordEncoder.encode("skm@8115");
                krceAdmin.setPassword(freshHash);
                krceAdmin.setRole("ROLE_COLLEGE_ADMIN");
                krceAdmin.setTenant(tenantRepository.findByTenantId("krce").orElse(null));
                userRepository.save(krceAdmin);
                logger.info("KRCE Admin password re-synced on startup.");
            } else {
                logger.info("Initializing Master Database with KRCE Admin: coe1@krce.ac.in");
                AppUser krceAdmin = new AppUser();
                krceAdmin.setId(UUID.randomUUID());
                krceAdmin.setEmail("coe1@krce.ac.in");
                krceAdmin.setPassword(passwordEncoder.encode("skm@8115"));
                krceAdmin.setRole("ROLE_COLLEGE_ADMIN");
                krceAdmin.setTenant(tenantRepository.findByTenantId("krce").orElse(null));
                userRepository.save(krceAdmin);
                logger.info("KRCE Admin created successfully.");
            }
        }

        // Ensure the Super Admin is present and password is always in sync
        {
            var existingAdmin = userRepository.findByEmail(adminUsername);
            if (existingAdmin.isPresent()) {
                AppUser superAdmin = existingAdmin.get();
                superAdmin.setPassword(passwordEncoder.encode(adminPassword));
                superAdmin.setRole("ROLE_SUPER_ADMIN");
                userRepository.save(superAdmin);
                logger.info("Super Admin password re-synced on startup: {}", adminUsername);
            } else {
                logger.info("Initializing Master Database with Super Admin: {}", adminUsername);
                AppUser superAdmin = new AppUser();
                superAdmin.setId(UUID.randomUUID());
                superAdmin.setEmail(adminUsername);
                superAdmin.setPassword(passwordEncoder.encode(adminPassword));
                superAdmin.setRole("ROLE_SUPER_ADMIN");
                userRepository.save(superAdmin);
                logger.info("Super Admin created successfully: {}", adminUsername);
            }
        }

        // --- Seed Mock Faculty for testing Duty Engine ---
        if (facultyRepository.count() == 0) {
            logger.info("Seeding 20 mock Faculty members for Invigilator Duty testing...");
            String[] depts = {"CSE", "ECE", "EEE", "MECH", "CIVIL", "IT"};
            java.util.List<com.exam.entity.Faculty> mockFaculty = new java.util.ArrayList<>();
            
            for (int i = 1; i <= 20; i++) {
                com.exam.entity.Faculty f = new com.exam.entity.Faculty(
                    UUID.randomUUID(),
                    "Prof. Faculty " + i,
                    "EMP10" + String.format("%02d", i),
                    depts[i % depts.length],
                    "Assistant Professor",
                    "98765432" + String.format("%02d", i),
                    "faculty" + i + "@krce.ac.in",
                    "K. RAMAKRISHNAN COLLEGE OF ENGINEERING",
                    true,
                    true
                );
                mockFaculty.add(f);
            }
            facultyRepository.saveAll(mockFaculty);
            logger.info("Successfully seeded 20 mock Faculty members.");
        }
    }
}
