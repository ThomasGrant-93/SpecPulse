package com.specpulse.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundUrlSecurityTest {

    @Test
    @DisplayName("Should reject localhost")
    void shouldRejectLocalhost() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");

        assertThatThrownBy(() -> security.validateOpenApiUrl("http://localhost:8080/openapi.json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject loopback")
    void shouldRejectLoopback() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");

        assertThatThrownBy(() -> security.validateOpenApiUrl("http://127.0.0.1/openapi.json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject cloud metadata")
    void shouldRejectMetadataIp() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");

        assertThatThrownBy(() -> security.validateOpenApiUrl("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject private IPv4")
    void shouldRejectPrivateIpv4() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");

        assertThatThrownBy(() -> security.validateOpenApiUrl("http://10.0.0.1/openapi.json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should accept public IP literal")
    void shouldAcceptPublicIpLiteral() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");

        // 1.2.3.4 is public; this test avoids DNS by using an IP literal.
        security.validateOpenApiUrl("http://1.2.3.4/openapi.json");
    }
}
