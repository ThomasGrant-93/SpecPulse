package com.specpulse.client;

import java.util.List;

public record OpenApiValidationResult(
        boolean success,
        List<String> errors
) {
}
