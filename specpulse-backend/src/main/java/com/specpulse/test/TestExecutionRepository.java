package com.specpulse.test;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecutionEntity, Long> {

    List<TestExecutionEntity> findByServiceIdOrderByExecutedAtDesc(Long serviceId);

    List<TestExecutionEntity> findBySpecVersionIdOrderByExecutedAtDesc(Long specVersionId);

    List<TestExecutionEntity> findByServiceIdAndStatusOrderByExecutedAtDesc(Long serviceId, String status);
}
