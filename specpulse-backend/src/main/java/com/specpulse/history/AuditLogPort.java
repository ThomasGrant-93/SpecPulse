package com.specpulse.history;

public interface AuditLogPort {

    void logEvent(Long serviceId, Long specVersionId, AuditEventType eventType, String details);

    void logEvent(Long serviceId, AuditEventType eventType, String details);
}
