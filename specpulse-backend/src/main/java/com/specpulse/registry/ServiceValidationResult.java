package com.specpulse.registry;

import java.util.List;

public record ServiceValidationResult(
        boolean valid,
        List<String> errors
) {
}
