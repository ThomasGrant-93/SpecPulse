package com.specpulse.test;

import java.time.Instant;

public record TestExecutionDTO(
        Long id,
        Long serviceId,
        Long specVersionId,
        String testName,
        String endpoint,
        String method,
        String status,
        Integer responseCode,
        Long responseTimeMs,
        String errorMessage,
        Instant executedAt
) {
    public static TestExecutionDTO fromEntity(TestExecutionEntity entity) {
        return new TestExecutionDTO(
                entity.getId(),
                entity.getServiceId(),
                entity.getSpecVersionId(),
                entity.getTestName(),
                entity.getEndpoint(),
                entity.getMethod(),
                entity.getStatus(),
                entity.getResponseCode(),
                entity.getResponseTimeMs(),
                entity.getErrorMessage(),
                entity.getExecutedAt()
        );
    }
}
