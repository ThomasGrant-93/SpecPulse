package com.specpulse.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class RefreshCsrfFilter extends OncePerRequestFilter {

    private static final List<String> PROTECTED_PATHS = List.of(
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!PROTECTED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // CSRF hardening for cookie-based refresh.
        // We validate Origin/Referer when present (browser cross-site requests typically include them).
        // If neither header is present, we allow (browser behavior varies for same-origin).
        String originHeader = request.getHeader("Origin");
        if (originHeader != null && !originHeader.isBlank()) {
            if (!isSameOrigin(originHeader, request)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String refererHeader = request.getHeader("Referer");
        if (refererHeader != null && !refererHeader.isBlank()) {
            if (!isSameOrigin(refererHeader, request)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSameOrigin(String absoluteUrlOrOriginHeader, HttpServletRequest request) {
        URI parsed;
        try {
            parsed = URI.create(absoluteUrlOrOriginHeader);
        } catch (RuntimeException e) {
            return false;
        }

        String expectedScheme = expectedScheme(request);
        String expectedHost = expectedHost(request);

        if (parsed.getScheme() == null || parsed.getHost() == null) {
            return false;
        }

        return Objects.equals(parsed.getScheme().toLowerCase(Locale.ROOT), expectedScheme)
                && Objects.equals(parsed.getHost().toLowerCase(Locale.ROOT), expectedHost);
    }

    private String expectedScheme(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return forwardedProto.toLowerCase(Locale.ROOT);
        }
        return request.isSecure() ? "https" : "http";
    }

    private String expectedHost(HttpServletRequest request) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            return stripPort(forwardedHost);
        }
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            return stripPort(hostHeader);
        }
        return request.getServerName();
    }

    private String stripPort(String hostMaybeWithPort) {
        int colon = hostMaybeWithPort.indexOf(':');
        if (colon > 0) {
            return hostMaybeWithPort.substring(0, colon);
        }
        return hostMaybeWithPort;
    }
}
