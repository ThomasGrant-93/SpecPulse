package com.specpulse.diff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecDiffRepository extends JpaRepository<SpecDiffEntity, Long> {

    List<SpecDiffEntity> findByServiceIdOrderByCreatedAtDesc(Long serviceId);

    List<SpecDiffEntity> findByFromVersionIdOrToVersionId(Long fromVersionId, Long toVersionId);
}
