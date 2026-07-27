package com.specpulse.proxy;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Request to proxy an outbound HTTP call.
 */
public record ExternalApiProxyRequest(
        String url,
        String method,
        Map<String, String> headers,
        JsonNode body
) {
}
