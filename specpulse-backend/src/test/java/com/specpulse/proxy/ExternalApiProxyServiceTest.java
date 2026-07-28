package com.specpulse.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
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

    @Test
    void shouldRejectLargeRequestBody() {
        OutboundUrlSecurity security = new OutboundUrlSecurity(true, "");
        ExternalApiProxyService service = new ExternalApiProxyService(security, new ObjectMapper());

        // ExternalApiProxyService currently caps request bodies at 2_000_000 bytes.
        String large = "a".repeat(2_000_001);

        ExternalApiProxyResponse res = service.proxy(new ExternalApiProxyRequest(
                "https://example.com/openapi.json",
                "POST",
                java.util.Map.of("Content-Type", "text/plain"),
                TextNode.valueOf(large)
        ));

        assertThat(res.status()).isEqualTo(0);
        assertThat(res.error()).contains("Request body is too large");
    }
}
