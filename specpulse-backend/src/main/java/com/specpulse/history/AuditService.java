package com.specpulse.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditService implements AuditLogPort, AuditReadPort {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public void logEvent(Long serviceId, Long specVersionId, AuditEventType eventType, String details) {
        AuditLogEntity auditLog = new AuditLogEntity(serviceId, specVersionId, eventType.name(), details);
        repository.save(auditLog);
        log.debug("Audit log: {} for service={}, version={}", eventType, serviceId, specVersionId);
    }

    @Transactional
    @Override
    public void logEvent(Long serviceId, AuditEventType eventType, String details) {
        logEvent(serviceId, null, eventType, details);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByServiceId(Long serviceId) {
        return repository.findByServiceIdOrderByCreatedAtDesc(serviceId).stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByEventType(AuditEventType eventType) {
        return repository.findByEventTypeOrderByCreatedAtDesc(eventType.name()).stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getRecentAuditLogs(int limit) {
        return repository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .map(AuditLogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsSince(Instant since) {
        return repository.findByCreatedAtAfter(since).stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }

}
