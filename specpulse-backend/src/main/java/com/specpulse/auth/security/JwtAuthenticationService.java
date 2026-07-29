package com.specpulse.auth.security;

import com.specpulse.auth.entity.RoleEntity;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Authentication business logic.
 *
 * Kept out of {@link JwtAuthenticationFilter} so the filter doesn't need to deal with JPA/lazy loading concerns.
 */
public class JwtAuthenticationService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<JwtPrincipal> authenticateAccessToken(String token) {
        return jwtService.parseAccessToken(token)
                .flatMap(principal -> userRepository.findByIdAndDeletedAtIsNull(principal.getUserId())
                        .filter(UserEntity::isEnabled)
                        .map(user -> buildPrincipal(user, principal)));
    }

    private JwtPrincipal buildPrincipal(UserEntity user, JwtPrincipal tokenPrincipal) {
        var enabledRoles = user.getRoles().stream()
                .filter(r -> r.getDeletedAt() == null && r.isEnabled())
                .map(RoleEntity::getName)
                .toList();

        var authorities = enabledRoles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        return new JwtPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                authorities,
                enabledRoles,
                tokenPrincipal.getAttributes()
        );
    }
}
