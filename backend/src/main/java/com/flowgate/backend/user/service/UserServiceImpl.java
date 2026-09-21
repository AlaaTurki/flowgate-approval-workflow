package com.flowgate.backend.user.service;

import com.flowgate.backend.common.exception.ConflictException;
import com.flowgate.backend.common.exception.NotFoundException;
import com.flowgate.backend.user.dto.RegisterRequest;
import com.flowgate.backend.user.dto.UpdateUserRequest;
import com.flowgate.backend.user.dto.UserDto;
import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.mapper.UserMapper;
import com.flowgate.backend.user.repository.RoleRepository;
import com.flowgate.backend.user.repository.UserRepository;
import com.flowgate.backend.workflow.repository.ApprovalActionRepository;
import com.flowgate.backend.workflow.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RequestRepository requestRepository;
    private final ApprovalActionRepository approvalActionRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                          RoleRepository roleRepository,
                          UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          RequestRepository requestRepository,
                          ApprovalActionRepository approvalActionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.requestRepository = requestRepository;
        this.approvalActionRepository = approvalActionRepository;
    }

    @Override
    public UserDto register(RegisterRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String email = req.getEmail() == null ? "" : req.getEmail().trim();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ConflictException("username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .fullName(req.getFullName())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .enabled(true)
                .createdAt(OffsetDateTime.now())
                .roles(resolveRoles(req.getRoles(), true))
                .build();

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public Optional<UserDto> findByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::toDto);
    }

    @Override
    public Optional<UserDto> findById(java.util.UUID id) {
        return userRepository.findById(id).map(userMapper::toDto);
    }

    @Override
    public List<UserDto> findAll() {
        List<User> all = userRepository.findAll();
        List<UserDto> dtos = new ArrayList<>(all.size());
        for (User u : all) dtos.add(userMapper.toDto(u));
        return dtos;
    }

    @Override
    public UserDto updateUser(java.util.UUID id, UpdateUserRequest dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String username = dto.getUsername().trim();
            if (!username.equals(user.getUsername()) && userRepository.findByUsername(username).isPresent()) {
                throw new ConflictException("username already exists");
            }
            user.setUsername(username);
        }
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String email = dto.getEmail().trim();
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
                throw new ConflictException("email already exists");
            }
            user.setEmail(email);
        }
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }
        if (dto.getRoles() != null) {
            user.setRoles(resolveRoles(dto.getRoles(), false));
        }

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public void deleteUser(java.util.UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        long requestCount = requestRepository.countBySubmittedBy(user);
        long actionCount = approvalActionRepository.countByActor(user);
        if (requestCount > 0 || actionCount > 0) {
            user.setEnabled(false);
            userRepository.save(user);
            return;
        }
        userRepository.delete(user);
    }

    private Set<Role> resolveRoles(Collection<String> rawRoles, boolean defaultEmployee) {
        Set<Role> roles = new HashSet<>();
        if (rawRoles == null || rawRoles.isEmpty()) {
            if (defaultEmployee) {
                roleRepository.findByName("ROLE_EMPLOYEE").ifPresent(roles::add);
            }
            return roles;
        }

        for (String rawRole : rawRoles) {
            if (rawRole == null || rawRole.isBlank()) {
                continue;
            }
            String normalizedRole = normalizeRole(rawRole);
            Role role = roleRepository.findByName(normalizedRole)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + rawRole));
            roles.add(role);
        }

        if (roles.isEmpty() && defaultEmployee) {
            roleRepository.findByName("ROLE_EMPLOYEE").ifPresent(roles::add);
        }
        return roles;
    }

    private String normalizeRole(String rawRole) {
        String trimmed = rawRole.trim();
        return trimmed.startsWith("ROLE_") ? trimmed.toUpperCase(Locale.ROOT) : "ROLE_" + trimmed.toUpperCase(Locale.ROOT);
    }
}

