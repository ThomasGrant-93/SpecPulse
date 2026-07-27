package com.specpulse.history;

import java.time.Instant;
import java.util.List;

public interface AuditReadPort {

    List<AuditLogDTO> getAuditLogsByServiceId(Long serviceId);

    List<AuditLogDTO> getAuditLogsByEventType(AuditEventType eventType);

    List<AuditLogDTO> getRecentAuditLogs(int limit);

    List<AuditLogDTO> getAuditLogsSince(Instant since);
}
