package com.specpulse.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_ATTRS = "attrs";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final ObjectMapper objectMapper;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.signingKey = buildSigningKey(props.getSecret());
    }

    public TokenBundle issueAccessToken(
            Long userId,
            String username,
            String email,
            boolean enabled,
            List<String> roles,
            Set<String> permissions,
            Map<String, Object> attributes
    ) {
        Instant now = Instant.now();
        Date exp = Date.from(now.plusSeconds(props.getAccessTokenTtlSeconds()));

        String token = Jwts.builder()
                .setIssuer(props.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(exp)
                .setSubject(username)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .claim("perms", permissions)
                .claim(CLAIM_ATTRS, attributes)
                .claim("enabled", enabled)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return new TokenBundle(token, null);
    }

    public TokenBundle issueRefreshToken(
            Long userId,
            String username,
            String email,
            boolean enabled,
            List<String> roles,
            Set<String> permissions,
            Map<String, Object> attributes
    ) {
        Instant now = Instant.now();
        Date exp = Date.from(now.plusSeconds(props.getRefreshTokenTtlSeconds()));

        String token = Jwts.builder()
                .setIssuer(props.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(exp)
                .setSubject(username)
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .claim("perms", permissions)
                .claim(CLAIM_ATTRS, attributes)
                .claim("enabled", enabled)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return new TokenBundle(null, token);
    }

    public Optional<JwtPrincipal> parseAccessToken(String token) {
        return parse(token, TOKEN_TYPE_ACCESS);
    }

    public Optional<JwtPrincipal> parseRefreshToken(String token) {
        return parse(token, TOKEN_TYPE_REFRESH);
    }

    private Optional<JwtPrincipal> parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String type = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.equals(type)) {
                return Optional.empty();
            }

            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String username = claims.get(CLAIM_USERNAME, String.class);
            String email = claims.get(CLAIM_EMAIL, String.class);
            Boolean enabled = claims.get("enabled", Boolean.class);

            List<String> roles = safeStringList(claims.get(CLAIM_ROLES));
            Set<String> permissions = safeStringSet(claims.get("perms"));
            Map<String, Object> attributes = objectMapper.convertValue(
                    claims.get(CLAIM_ATTRS),
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            // RBAC-only: grant ROLE_<roleName> authorities derived from token roles.
            var authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());

            return Optional.of(new JwtPrincipal(
                    userId,
                    username,
                    email,
                    enabled != null ? enabled : true,
                    authorities,
                    roles,
                    attributes != null ? attributes : Collections.emptyMap()
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private List<String> safeStringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(raw));
    }

    private Set<String> safeStringSet(Object raw) {
        if (raw == null) {
            return Set.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        if (raw instanceof Set<?> set) {
            return set.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of(String.valueOf(raw));
    }

    private SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("specpulse.auth.jwt.secret must be non-empty");
        }

        // If secret looks like base64, decode it; otherwise treat it as raw UTF-8 bytes.
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                return Keys.hmacShaKeyFor(decoded);
            }
        } catch (RuntimeException ignored) {
            // Not base64
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("specpulse.auth.jwt.secret must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public record TokenBundle(String accessToken, String refreshToken) {
    }
}
