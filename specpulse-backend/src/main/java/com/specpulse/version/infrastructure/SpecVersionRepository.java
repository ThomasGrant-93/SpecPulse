package com.specpulse.version.infrastructure;

import com.specpulse.version.domain.SpecVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpecVersionRepository extends JpaRepository<SpecVersionEntity, Long> {

    List<SpecVersionEntity> findByServiceIdOrderByPulledAtDesc(Long serviceId);

    Optional<SpecVersionEntity> findFirstByServiceIdOrderByPulledAtDesc(Long serviceId);

    // PostgreSQL-specific: pick the latest row per service_id in one query.
    @Query(
            value = "SELECT DISTINCT ON (sv.service_id) sv.* " +
                    "FROM spec_versions sv " +
                    "WHERE sv.service_id IN (:serviceIds) " +
                    "ORDER BY sv.service_id, sv.pulled_at DESC",
            nativeQuery = true
    )
    List<SpecVersionEntity> findLatestByServiceIds(@Param("serviceIds") Collection<Long> serviceIds);

}
