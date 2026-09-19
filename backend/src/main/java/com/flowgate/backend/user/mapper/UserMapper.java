package com.flowgate.backend.user.mapper;

import com.flowgate.backend.user.dto.UserDto;
import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setEnabled(user.isEnabled());
        dto.setRoles(user.getRoles() == null ? java.util.List.of() : user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .collect(Collectors.toList()));
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setEnabled(dto.isEnabled());
        user.setCreatedAt(dto.getCreatedAt());
        return user;
    }
}
