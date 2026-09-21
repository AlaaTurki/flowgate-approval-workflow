package com.flowgate.backend.user.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    private String username;

    @Email
    private String email;

    private String fullName;
    private Boolean enabled;
    private List<String> roles;
}
