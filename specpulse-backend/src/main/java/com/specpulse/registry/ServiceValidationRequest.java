package com.specpulse.registry;

public record ServiceValidationRequest(
        String name,
        String openApiUrl
) {
}
