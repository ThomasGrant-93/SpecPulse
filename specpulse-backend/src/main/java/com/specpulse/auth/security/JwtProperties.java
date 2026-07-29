package com.specpulse.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "specpulse.auth.jwt")
public class JwtProperties {

    /**
     * When false, Spring Security will not enforce authentication for API requests.
     * Controllers that still use legacy `specpulse.auth.token` checks behave as before.
     */
    private boolean enabled = true;

    /**
     * HS256 secret. Set this in production.
     * <p>
     * Dev default is intentionally non-empty so the app can start without extra config.
     */
    private String secret = "dev-change-me-dev-change-me-please-set-in-prod";

    private long accessTokenTtlSeconds = 900; // 15 minutes
    private long refreshTokenTtlSeconds = 2_592_000; // 30 days
    private String issuer = "specpulse";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
