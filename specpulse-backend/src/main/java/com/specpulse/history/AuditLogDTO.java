package com.specpulse.history;

import java.time.Instant;

import com.specpulse.history.domain.AuditLogEntity;

public record AuditLogDTO(
        Long id,
        Long serviceId,
        Long specVersionId,
        String eventType,
        String eventDetails,
        Instant createdAt
) {
    public static AuditLogDTO fromEntity(AuditLogEntity entity) {
        return new AuditLogDTO(
                entity.getId(),
                entity.getServiceId(),
                entity.getSpecVersionId(),
                entity.getEventType(),
                entity.getEventDetails(),
                entity.getCreatedAt()
        );
    }
}
