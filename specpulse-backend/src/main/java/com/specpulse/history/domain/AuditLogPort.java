package com.specpulse.history.domain;

import com.specpulse.history.AuditEventType;

public interface AuditLogPort {

    void logEvent(Long serviceId, Long specVersionId, AuditEventType eventType, String details);

    void logEvent(Long serviceId, AuditEventType eventType, String details);
}
