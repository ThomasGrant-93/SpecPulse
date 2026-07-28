package com.specpulse.proxy;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Response from outbound proxy call.
 * <p>
 * Note: This is intentionally modeled to match what the frontend API tester expects.
 */
public record ExternalApiProxyResponse(
        int status,
        String statusText,
        Map<String, String> headers,
        JsonNode body,
        String error
) {
}
