package com.flowgate.backend.startup;

import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.repository.RoleRepository;
import com.flowgate.backend.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

@Component
public class StartupDataLoader implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StartupDataLoader(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Ensure default roles exist (already seeded by Flyway but safe to check)
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(Role.builder().id(UUID.randomUUID()).name("ROLE_ADMIN").description("Administrator").build()));
        Role managerRole = roleRepository.findByName("ROLE_MANAGER").orElseGet(() -> roleRepository.save(Role.builder().id(UUID.randomUUID()).name("ROLE_MANAGER").description("Manager").build()));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(Role.builder().id(UUID.randomUUID()).name("ROLE_EMPLOYEE").description("Employee").build()));

        // Seed users if missing
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .id(UUID.randomUUID())
                    .username("admin")
                    .email("admin@flowgate.local")
                    .fullName("System Administrator")
                    .passwordHash(passwordEncoder.encode("adminpass"))
                    .enabled(true)
                    .createdAt(OffsetDateTime.now())
                    .roles(new HashSet<>())
                    .build();
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
        }

        if (userRepository.findByUsername("manager").isEmpty()) {
            User manager = User.builder()
                    .id(UUID.randomUUID())
                    .username("manager")
                    .email("manager@flowgate.local")
                    .fullName("Default Manager")
                    .passwordHash(passwordEncoder.encode("managerpass"))
                    .enabled(true)
                    .createdAt(OffsetDateTime.now())
                    .roles(new HashSet<>())
                    .build();
            manager.getRoles().add(managerRole);
            userRepository.save(manager);
        }

        if (userRepository.findByUsername("employee").isEmpty()) {
            User employee = User.builder()
                    .id(UUID.randomUUID())
                    .username("employee")
                    .email("employee@flowgate.local")
                    .fullName("Default Employee")
                    .passwordHash(passwordEncoder.encode("employeepass"))
                    .enabled(true)
                    .createdAt(OffsetDateTime.now())
                    .roles(new HashSet<>())
                    .build();
            employee.getRoles().add(employeeRole);
            userRepository.save(employee);
        }
    }
}
