package com.examportal.init;

import com.examportal.entity.Role;
import com.examportal.entity.User;
import com.examportal.repository.RoleRepository;
import com.examportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Database Initializer
 * 
 * Seeds initial roles and admin user on application startup
 * Only runs if roles don't exist (idempotent)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeAdminUser();
        log.info("Database initialization complete!");
    }

    private void initializeRoles() {
        // Create STUDENT role
        if (roleRepository.findByName(Role.STUDENT).isEmpty()) {
            Role studentRole = Role.builder()
                .name(Role.STUDENT)
                .description("Student role - can take exams and view own submissions")
                .build();
            roleRepository.save(studentRole);
            log.info("Created STUDENT role");
        }

        // Create MODERATOR role
        if (roleRepository.findByName(Role.MODERATOR).isEmpty()) {
            Role moderatorRole = Role.builder()
                .name(Role.MODERATOR)
                .description("Moderator role - can create and monitor exams (department-restricted)")
                .build();
            roleRepository.save(moderatorRole);
            log.info("Created MODERATOR role");
        }

        // Create ADMIN role
        if (roleRepository.findByName(Role.ADMIN).isEmpty()) {
            Role adminRole = Role.builder()
                .name(Role.ADMIN)
                .description("Admin role - full system access")
                .build();
            roleRepository.save(adminRole);
            log.info("Created ADMIN role");
        }
    }

    private void initializeAdminUser() {
        String adminEmail = "admin@examportal.com";
        
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);

            User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin@123"))
                .fullName("System Administrator")
                .department("ADMIN")
                .enabled(true)
                .accountNonLocked(true)
                .roles(roles)
                .build();

            userRepository.save(admin);
            log.info("Created admin user - Email: {}, Password: Admin@123", adminEmail);
        }
    }
}
