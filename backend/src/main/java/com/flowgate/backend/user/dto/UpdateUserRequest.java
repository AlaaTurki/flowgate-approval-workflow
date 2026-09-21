package com.flowgate.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
    private String username;

    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 100)
    private String fullName;
    private Boolean enabled;
    private List<String> roles;
}
