package com.specpulse.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.auth.repository.RefreshTokenRepository;
import com.specpulse.auth.security.JwtService;
import com.specpulse.auth.security.JwtProperties;
import com.specpulse.auth.security.TokenHashUtil;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.entity.RefreshTokenEntity;
import com.specpulse.auth.entity.RoleEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
    }

    private List<String> enabledRoleNames(UserEntity user) {
        return user.getRoles().stream()
                .filter(role -> role.getDeletedAt() == null && role.isEnabled())
                .map(RoleEntity::getName)
                .collect(Collectors.toList());
    }

    private Map<String, Object> userAttributes(UserEntity user) {
        return objectMapper.convertValue(
                user.getAttributes(),
                new TypeReference<Map<String, Object>>() {
                }
        );
    }

    @Transactional
    public JwtService.TokenBundle login(String username, String password) {
        UserEntity user = userRepository.findByUsernameAndEnabledTrueAndDeletedAtIsNull(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username/password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username/password");
        }

        var roles = enabledRoleNames(user);

        Set<String> permissions = Set.of();

        Map<String, Object> attrs = userAttributes(user);

        boolean enabled = user.isEnabled();

        JwtService.TokenBundle access = jwtService.issueAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                enabled,
                roles,
                permissions,
                attrs
        );

        JwtService.TokenBundle refresh = jwtService.issueRefreshToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                enabled,
                roles,
                permissions,
                attrs
        );

        // Persist refresh token so we can rotate/revoke.
        String refreshToken = refresh.refreshToken();
        String refreshHash = TokenHashUtil.sha256Hex(refreshToken);
        Instant now = Instant.now();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(now.plusSeconds(jwtProperties.getRefreshTokenTtlSeconds()), java.time.ZoneOffset.UTC);

        RefreshTokenEntity refreshEntity = new RefreshTokenEntity();
        refreshEntity.setUser(user);
        refreshEntity.setTokenHash(refreshHash);
        refreshEntity.setRevokedAt(null);
        refreshEntity.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshEntity);

        return new JwtService.TokenBundle(access.accessToken(), refresh.refreshToken());
    }

    @Transactional
    public JwtService.TokenBundle refreshAccessToken(String refreshToken) {
        var principalOpt = jwtService.parseRefreshToken(refreshToken);
        var principal = principalOpt.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Token rotation / replay protection.
        String refreshHash = TokenHashUtil.sha256Hex(refreshToken);
        LocalDateTime now = LocalDateTime.now(java.time.Clock.systemUTC());
        RefreshTokenEntity stored = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(refreshHash, now)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // revoke old refresh token
        stored.setRevokedAt(now);
        refreshTokenRepository.save(stored);

        var roles = enabledRoleNames(user);

        Set<String> permissions = Set.of();

        Map<String, Object> attrs = userAttributes(user);

        boolean enabled = user.isEnabled();

        JwtService.TokenBundle access = jwtService.issueAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                enabled,
                roles,
                permissions,
                attrs
        );

        JwtService.TokenBundle refresh = jwtService.issueRefreshToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                enabled,
                roles,
                permissions,
                attrs
        );

        // persist new refresh token (rotation)
        String newRefreshToken = refresh.refreshToken();
        String newRefreshHash = TokenHashUtil.sha256Hex(newRefreshToken);
        Instant t = Instant.now();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(t.plusSeconds(jwtProperties.getRefreshTokenTtlSeconds()), java.time.ZoneOffset.UTC);

        RefreshTokenEntity refreshEntity = new RefreshTokenEntity();
        refreshEntity.setUser(user);
        refreshEntity.setTokenHash(newRefreshHash);
        refreshEntity.setRevokedAt(null);
        refreshEntity.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshEntity);

        return new JwtService.TokenBundle(access.accessToken(), refresh.refreshToken());
    }

}
