package com.specpulse.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logEvent(Long serviceId, Long specVersionId, EventType eventType, String details) {
        AuditLogEntity auditLog = new AuditLogEntity(serviceId, specVersionId, eventType.name(), details);
        repository.save(auditLog);
        log.debug("Audit log: {} for service={}, version={}", eventType, serviceId, specVersionId);
    }

    @Transactional
    public void logEvent(Long serviceId, EventType eventType, String details) {
        logEvent(serviceId, null, eventType, details);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByServiceId(Long serviceId) {
        return repository.findByServiceIdOrderByCreatedAtDesc(serviceId).stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByEventType(EventType eventType) {
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

    public enum EventType {
        SERVICE_CREATED,
        SERVICE_UPDATED,
        SERVICE_DELETED,
        SPEC_FETCHED,
        SPEC_FETCH_FAILED,
        SPEC_VERSION_CREATED,
        SPEC_VERSION_SKIPPED, // Hash unchanged
        DIFF_ANALYZED,
        BREAKING_CHANGE_DETECTED,
        TEST_EXECUTED,
        TEST_FAILED
    }
}
