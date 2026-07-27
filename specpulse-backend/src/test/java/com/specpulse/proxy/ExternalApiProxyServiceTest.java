package com.specpulse.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.client.OutboundUrlSecurity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalApiProxyServiceTest {

    @Test
    void shouldRejectLocalhost() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");
        ExternalApiProxyService service = new ExternalApiProxyService(security, new ObjectMapper());

        ExternalApiProxyResponse res = service.proxy(new ExternalApiProxyRequest(
                "http://localhost:8080/openapi.json",
                "GET",
                null,
                null
        ));

        assertThat(res.status()).isEqualTo(0);
        assertThat(res.error()).isNotBlank();
    }

    @Test
    void shouldRejectUnsupportedMethod() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");
        ExternalApiProxyService service = new ExternalApiProxyService(security, new ObjectMapper());

        ExternalApiProxyResponse res = service.proxy(new ExternalApiProxyRequest(
                "https://example.com/openapi.json",
                "CONNECT",
                null,
                null
        ));

        assertThat(res.status()).isEqualTo(0);
        assertThat(res.error()).contains("Unsupported method");
    }
}
