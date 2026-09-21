package com.flowgate.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
    private String username;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(min = 8, max = 72)
    // at least one letter and one digit
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
    private String password;

    private List<String> roles;
}
