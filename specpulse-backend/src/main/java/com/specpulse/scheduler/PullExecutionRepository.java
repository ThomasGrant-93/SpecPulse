package com.specpulse.scheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PullExecutionRepository extends JpaRepository<PullExecutionEntity, Long> {

    List<PullExecutionEntity> findByServiceIdOrderByExecutedAtDesc(Long serviceId);

    List<PullExecutionEntity> findByExecutedAtAfter(Instant since);

    List<PullExecutionEntity> findByServiceIdAndStatusOrderByExecutedAtDesc(Long serviceId, String status);
}
