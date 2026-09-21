package com.flowgate.backend.security;

import com.flowgate.backend.user.entity.Role;
import com.flowgate.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    private final Environment environment;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @jakarta.annotation.PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // if running with the dev profile, try to load from a repository .env or generate a dev secret
            String[] active = environment.getActiveProfiles();
            boolean dev = false;
            for (String p : active) {
                if ("dev".equalsIgnoreCase(p)) {
                    dev = true;
                    break;
                }
            }

            if (dev) {
                // try to locate a .env or .env.example up the directory tree
                String loaded = tryLoadSecretFromDotEnv();
                if (loaded != null && !loaded.isBlank()) {
                    secret = loaded.trim();
                    log.warn("Loaded JWT_SECRET from .env for dev profile (in-memory). Do not use in production.");
                } else {
                    // generate a temporary secret for dev convenience
                    SecureRandom rnd = new SecureRandom();
                    byte[] bytes = new byte[48];
                    rnd.nextBytes(bytes);
                    secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    log.warn("No JWT_SECRET found for dev profile; generated a temporary secret (in-memory). Restart will invalidate tokens.");
                }
            }
        }

        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret must be set and at least 32 bytes long");
        }
    }

    private String tryLoadSecretFromDotEnv() {
        Path cwd = Path.of("");
        Path cur = cwd.toAbsolutePath();
        for (int i = 0; i < 6 && cur != null; i++) {
            Path envFile = cur.resolve(".env");
            if (Files.exists(envFile)) {
                try {
                    for (String line : Files.readAllLines(envFile)) {
                        String l = line.trim();
                        if (l.startsWith("JWT_SECRET=")) {
                            return l.substring("JWT_SECRET=".length()).trim();
                        }
                    }
                } catch (IOException e) {
                    // ignore and continue
                }
            }
            Path envEx = cur.resolve(".env.example");
            if (Files.exists(envEx)) {
                try {
                    for (String line : Files.readAllLines(envEx)) {
                        String l = line.trim();
                        if (l.startsWith("JWT_SECRET=")) {
                            return l.substring("JWT_SECRET=".length()).trim();
                        }
                    }
                } catch (IOException e) {
                    // ignore and continue
                }
            }
            cur = cur.getParent();
        }
        return null;
    }

    public String generateToken(User user) {
        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles().stream()
                .map(Role::getName)
                .toList();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String id = parseClaims(token).get("userId", String.class);
        return id == null ? null : UUID.fromString(id);
    }

    public List<String> extractRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid or expired JWT", ex);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
