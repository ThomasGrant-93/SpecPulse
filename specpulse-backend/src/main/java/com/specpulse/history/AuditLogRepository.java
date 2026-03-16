package com.specpulse.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByServiceIdOrderByCreatedAtDesc(Long serviceId);

    List<AuditLogEntity> findByEventTypeOrderByCreatedAtDesc(String eventType);

    List<AuditLogEntity> findByCreatedAtAfter(Instant since);

    List<AuditLogEntity> findByServiceIdAndEventTypeOrderByCreatedAtDesc(Long serviceId, String eventType);
}
