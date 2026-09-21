package com.flowgate.backend.user.controller;

import com.flowgate.backend.security.JwtService;
import com.flowgate.backend.user.dto.AuthResponse;
import com.flowgate.backend.user.dto.LoginRequest;
import com.flowgate.backend.user.dto.RegisterRequest;
import com.flowgate.backend.user.dto.UserDto;
import com.flowgate.backend.user.entity.User;
import com.flowgate.backend.user.repository.UserRepository;
import com.flowgate.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@org.springframework.validation.annotation.Validated
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean registrationEnabled;

    @Autowired
    public AuthController(UserService userService,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         JwtService jwtService,
                         @Value("${app.registration.enabled:false}") boolean registrationEnabled) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.registrationEnabled = registrationEnabled;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Validated @RequestBody RegisterRequest req) {
        if (!registrationEnabled) {
            throw new IllegalArgumentException("Self-registration is disabled");
        }
        UserDto created = userService.register(req);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
    }
}
