package com.flowgate.backend.startup;

import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.repository.RoleRepository;
import com.flowgate.backend.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

@Component
@Profile("dev")
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
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Administrator").build()));
        Role managerRole = roleRepository.findByName("ROLE_MANAGER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_MANAGER").description("Manager").build()));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_EMPLOYEE").description("Employee").build()));

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@flowgate.local")
                    .fullName("System Administrator")
                    .passwordHash(passwordEncoder.encode("adminpass"))
                    .enabled(true)
                    .createdAt(OffsetDateTime.now())
                    .roles(new HashSet<>(java.util.Set.of(adminRole)))
                    .build();
            userRepository.saveAndFlush(admin);
        }

        if (userRepository.findByUsername("manager").isEmpty()) {
            User manager = User.builder()
                    .username("manager")
                    .email("manager@flowgate.local")
                    .fullName("Default Manager")
                    .passwordHash(passwordEncoder.encode("managerpass"))
                    .enabled(true)
                    .createdAt(OffsetDateTime.now())
                    .roles(new HashSet<>(java.util.Set.of(managerRole)))
                    .build();
            userRepository.saveAndFlush(manager);
        }

        if (userRepository.findByUsername("employee").isEmpty()) {
            User employee = User.builder()
                    .username("employee")
                    .email("employee@flowgate.local")
                    .fullName("Default Employee")
                    .passwordHash(passwordEncoder.encode("employeepass"))
                    .enabled(true)
                    .createdAt(OffsetDateTime.now())
                    .roles(new HashSet<>(java.util.Set.of(employeeRole)))
                    .build();
            userRepository.saveAndFlush(employee);
        }
    }
}
