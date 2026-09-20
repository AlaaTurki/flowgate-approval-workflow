package com.flowgate.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank
    private String username;

    @Email
    private String email;

    private String fullName;
    private Boolean enabled;
}
