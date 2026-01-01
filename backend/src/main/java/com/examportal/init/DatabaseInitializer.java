package com.examportal.init;

import com.examportal.entity.Role;
import com.examportal.entity.User;
import com.examportal.repository.RoleRepository;
import com.examportal.repository.UserRepository;
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
public class DatabaseInitializer implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseInitializer.class);
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeAdminUser();
        log.info("Database initialization complete!");
    }

    private void initializeRoles() {
        // Create STUDENT role
        if (roleRepository.findByName(Role.STUDENT).isEmpty()) {
            Role studentRole = new Role();
            studentRole.setName(Role.STUDENT);
            studentRole.setDescription("Student role - can take exams and view own submissions");
            roleRepository.save(studentRole);
            log.info("Created STUDENT role");
        }

        // Create MODERATOR role
        if (roleRepository.findByName(Role.MODERATOR).isEmpty()) {
            Role moderatorRole = new Role();
            moderatorRole.setName(Role.MODERATOR);
            moderatorRole.setDescription("Moderator role - can create and monitor exams (department-restricted)");
            roleRepository.save(moderatorRole);
            log.info("Created MODERATOR role");
        }

        // Create ADMIN role
        if (roleRepository.findByName(Role.ADMIN).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(Role.ADMIN);
            adminRole.setDescription("Admin role - full system access");
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

            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("System Administrator");
            admin.setDepartment("ADMIN");
            admin.setEnabled(true);
            admin.setAccountNonLocked(true);
            admin.setRoles(roles);

            userRepository.save(admin);
            log.info("Created admin user - Email: {}, Password: Admin@123", adminEmail);
        }
    }
}
