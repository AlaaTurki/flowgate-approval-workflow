package com.flowgate.backend.user.service;

import com.flowgate.backend.user.dto.UserDto;
import com.flowgate.backend.user.dto.RegisterRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    UserDto register(RegisterRequest req);
    Optional<UserDto> findByUsername(String username);
    Optional<UserDto> findById(UUID id);
    List<UserDto> findAll();
}
