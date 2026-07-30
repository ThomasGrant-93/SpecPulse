package com.specpulse.auth.security;

import com.specpulse.auth.entity.RoleEntity;
import com.specpulse.auth.entity.PermissionEntity;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
                        .filter(user -> user.isEnabled())
                        .map(user -> buildPrincipal(user, principal)));
    }

    private JwtPrincipal buildPrincipal(UserEntity user, JwtPrincipal tokenPrincipal) {
        List<RoleEntity> enabledRoles = user.getRoles().stream()
                .filter(r -> r.getDeletedAt() == null && r.isEnabled())
                .toList();

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();

        for (RoleEntity role : enabledRoles) {
            String roleName = role.getName();
            roleNames.add(roleName);
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

            for (PermissionEntity perm : role.getPermissions()) {
                if (perm.getDeletedAt() == null && perm.isEnabled()) {
                    authorities.add(new SimpleGrantedAuthority(RbacPermissions.authorityFor(perm.getName())));
                }
            }
        }

        return new JwtPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                authorities,
                roleNames,
                tokenPrincipal.getAttributes()
        );
    }
}
