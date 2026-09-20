package com.flowgate.backend.user.service;

import com.flowgate.backend.user.dto.RegisterRequest;
import com.flowgate.backend.user.dto.UpdateUserRequest;
import com.flowgate.backend.user.dto.UserDto;
import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.mapper.UserMapper;
import com.flowgate.backend.user.repository.RoleRepository;
import com.flowgate.backend.user.repository.UserRepository;
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

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDto register(RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("username_exists");
        }
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .fullName(req.getFullName())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .enabled(true)
                .createdAt(OffsetDateTime.now())
                .roles(new HashSet<>())
                .build();

        // default role: EMPLOYEE
        roleRepository.findByName("ROLE_EMPLOYEE").ifPresent(user.getRoles()::add);

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
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public void deleteUser(java.util.UUID id) {
        userRepository.deleteById(id);
    }
}

