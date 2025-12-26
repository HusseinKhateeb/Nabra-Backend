package com.nabra.backend.common.config;

import com.nabra.backend.common.model.Enums;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes database with seed data on application startup.
 * Creates default admin user if it doesn't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
    }

    /**
     * Creates a default admin user if no admin exists in the database.
     * This ensures the system has at least one admin account for management.
     */
    private void initializeAdminUser() {
        // Check if any admin user already exists
        boolean adminExists = userRepository.findByUsername("admin").isPresent();

        if (!adminExists) {
            log.info("Creating default admin user...");

            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@nabra.com");
            adminUser.setPasswordHash(passwordEncoder.encode("Admin@123456"));
            adminUser.setDisplayName("System Administrator");
            adminUser.setRole(Enums.UserRole.ADMIN);
            adminUser.setStatus(Enums.UserStatus.ACTIVE);
            adminUser.setUserType(Enums.UserType.INSTRUCTOR);
            adminUser.setEmailVerified(true);
            adminUser.setHighContrastEnabled(false);
            adminUser.setFontScale(1.0);
            adminUser.setVibrationEnabled(true);

            userRepository.save(adminUser);
            log.info("✓ Default admin user created successfully");
            log.info("  Username: admin");
            log.info("  Email: admin@nabra.com");
            log.info("  Password: Admin@123456");
            log.info("  Role: ADMIN");
        } else {
            log.info("✓ Admin user already exists, skipping initialization");
        }
    }
}
