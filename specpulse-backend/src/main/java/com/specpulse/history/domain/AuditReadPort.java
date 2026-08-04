package com.specpulse.history.domain;

import java.time.Instant;
import java.util.List;

import com.specpulse.history.AuditEventType;
import com.specpulse.history.AuditLogDTO;

public interface AuditReadPort {

    List<AuditLogDTO> getAuditLogsByServiceId(Long serviceId);

    List<AuditLogDTO> getAuditLogsByEventType(AuditEventType eventType);

    List<AuditLogDTO> getRecentAuditLogs(int limit);

    List<AuditLogDTO> getAuditLogsSince(Instant since);
}
