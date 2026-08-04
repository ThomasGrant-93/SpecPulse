package com.specpulse.auth.api;

import com.specpulse.auth.security.JwtPrincipal;
import com.specpulse.auth.security.JwtProperties;
import com.specpulse.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    private static final String REFRESH_COOKIE_NAME = "specpulse.refreshToken";
    private static final String REFRESH_COOKIE_SAMESITE = "Lax";

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            var tokens = authService.login(request.username(), request.password());

            // Store refresh token in an HttpOnly cookie to reduce XSS token theft risk.
            setRefreshCookie(httpRequest, httpResponse, tokens.refreshToken());
            return ResponseEntity.ok(new LoginResponse(tokens.accessToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                clearRefreshCookie(httpRequest, httpResponse);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            var tokens = authService.refreshAccessToken(refreshToken);
            setRefreshCookie(httpRequest, httpResponse, tokens.refreshToken());
            return ResponseEntity.ok(new RefreshResponse(tokens.accessToken()));
        } catch (IllegalArgumentException e) {
            clearRefreshCookie(httpRequest, httpResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        clearRefreshCookie(httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new MeResponse(
                principal.getUserId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.isEnabled(),
                principal.getRoles(),
                principal.getAuthorities().stream().map(a -> a.getAuthority()).toList(),
                principal.getAttributes()
        ));
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String accessToken
    ) {
    }

    public record RefreshResponse(
            String accessToken
    ) {
    }

    public record MeResponse(
            Long userId,
            String username,
            String email,
            boolean enabled,
            List<String> roles,
            List<String> permissions,
            Map<String, Object> attributes
    ) {
    }

    private void setRefreshCookie(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            String refreshToken
    ) {
        boolean secure = httpRequest.isSecure();

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(REFRESH_COOKIE_SAMESITE)
                .path("/api/v1/auth")
                .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds()))
                .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        boolean secure = httpRequest.isSecure();

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(REFRESH_COOKIE_SAMESITE)
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
