package com.flowgate.backend.user.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private boolean enabled;
    private OffsetDateTime createdAt;
}
