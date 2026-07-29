package com.specpulse.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.auth.security.JwtPrincipal;
import com.specpulse.auth.security.JwtService;
import com.specpulse.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService, JwtService jwtService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            var tokens = authService.login(request.username(), request.password());
            return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            JwtService.TokenBundle tokens = authService.refreshAccessToken(request.refreshToken());
            return ResponseEntity.ok(new RefreshResponse(tokens.accessToken(), tokens.refreshToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
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
            String accessToken,
            String refreshToken
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record RefreshResponse(
            String accessToken,
            String refreshToken
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
}
