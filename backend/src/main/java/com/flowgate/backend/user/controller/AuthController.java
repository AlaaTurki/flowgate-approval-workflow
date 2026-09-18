package com.flowgate.backend.user.controller;

import com.flowgate.backend.user.dto.AuthResponse;
import com.flowgate.backend.user.dto.LoginRequest;
import com.flowgate.backend.user.dto.RegisterRequest;
import com.flowgate.backend.user.dto.UserDto;
import com.flowgate.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Validated @RequestBody RegisterRequest req) {
        UserDto created = userService.register(req);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest req) {
        var userOpt = userService.findByUsername(req.getUsername());
        if (userOpt.isPresent()) {
            // Simple stub: password check and JWT will be implemented in Phase 3.
            return ResponseEntity.ok(new AuthResponse("token-placeholder", "Bearer"));
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
