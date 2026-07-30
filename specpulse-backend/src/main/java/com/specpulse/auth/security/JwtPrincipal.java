package com.specpulse.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JwtPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final List<String> roles;
    private final Map<String, Object> attributes;

    public JwtPrincipal(
            Long userId,
            String username,
            String email,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities,
            List<String> roles,
            Map<String, Object> attributes
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.enabled = enabled;
        this.authorities = authorities;
        this.roles = roles;
        this.attributes = attributes;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
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
