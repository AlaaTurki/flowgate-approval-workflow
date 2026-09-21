package com.flowgate.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {
    private final UUID id;
    private final String username;
    private final List<SimpleGrantedAuthority> authorities;
    private final boolean enabled;

    public AuthenticatedUser(UUID id, String username, List<String> roles, boolean enabled) {
        this.id = id;
        this.username = username;
        this.authorities = (roles == null ? List.<String>of() : roles).stream()
                .filter(role -> role != null && !role.isBlank())
                .map(SimpleGrantedAuthority::new)
                .toList();
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
