package com.examportal.init;

import com.examportal.entity.Role;
import com.examportal.entity.User;
import com.examportal.entity.UserRole;
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

    public DatabaseInitializer(RoleRepository roleRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeAdminUser();
        initializeModeratorUser();
        initializeTestStudent();
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
            admin.setUsername("admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setProfile("ADMIN");
            admin.setEnabled(true);

            // Create UserRole association
            UserRole userRole = new UserRole();
            userRole.setUser(admin);
            userRole.setRole(adminRole);
            admin.getUserRoles().add(userRole);

            userRepository.save(admin);
            log.info("Created admin user - Email: {}, Password: admin123", adminEmail);
        }
    }

    private void initializeModeratorUser() {
        String modEmail = "moderator@examportal.com";

        if (userRepository.findByEmail(modEmail).isEmpty()) {
            Role modRole = roleRepository.findByName(Role.MODERATOR)
                    .orElseThrow(() -> new RuntimeException("Moderator role not found"));

            User moderator = new User();
            moderator.setUsername("moderator");
            moderator.setEmail(modEmail);
            moderator.setPassword(passwordEncoder.encode("moderator123"));
            moderator.setFirstName("Test");
            moderator.setLastName("Moderator");
            moderator.setDepartment("CSE");
            moderator.setProfile("MODERATOR");
            moderator.setEnabled(true);

            UserRole userRole = new UserRole();
            userRole.setUser(moderator);
            userRole.setRole(modRole);
            moderator.getUserRoles().add(userRole);

            userRepository.save(moderator);
            log.info("Created test moderator - Email: {}, Password: moderator123", modEmail);
        }
    }

    private void initializeTestStudent() {
        String studentEmail = "student@examportal.com";

        if (userRepository.findByEmail(studentEmail).isEmpty()) {
            Role studentRole = roleRepository.findByName(Role.STUDENT)
                    .orElseThrow(() -> new RuntimeException("Student role not found"));

            User student = new User();
            student.setUsername("student");
            student.setEmail(studentEmail);
            student.setPassword(passwordEncoder.encode("student123"));
            student.setFirstName("Test");
            student.setLastName("Student");
            student.setDepartment("CSE");
            student.setProfile("STUDENT");
            student.setEnabled(true);

            // Create UserRole association
            UserRole userRole = new UserRole();
            userRole.setUser(student);
            userRole.setRole(studentRole);
            student.getUserRoles().add(userRole);

            userRepository.save(student);
            log.info("Created test student - Email: {}, Password: student123", studentEmail);
        }
    }
}
