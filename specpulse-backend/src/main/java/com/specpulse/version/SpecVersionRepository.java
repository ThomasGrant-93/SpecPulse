package com.specpulse.version;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecVersionRepository extends JpaRepository<SpecVersionEntity, Long> {

    List<SpecVersionEntity> findByServiceIdOrderByPulledAtDesc(Long serviceId);

    Optional<SpecVersionEntity> findByServiceIdAndVersionHash(Long serviceId, String versionHash);

    Optional<SpecVersionEntity> findFirstByServiceIdOrderByPulledAtDesc(Long serviceId);

    @Query("SELECT sv FROM SpecVersionEntity sv WHERE sv.service.id = :serviceId ORDER BY sv.pulledAt DESC LIMIT :limit")
    List<SpecVersionEntity> findByServiceIdLimit(@Param("serviceId") Long serviceId, @Param("limit") int limit);
}
