package com.specpulse.registry.infrastructure;

import com.specpulse.registry.domain.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    Optional<ServiceEntity> findByName(String name);

    List<ServiceEntity> findByEnabledTrue();

    List<ServiceEntity> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    boolean existsByName(String name);

    /**
     * Full-text search across name, description, and openApiUrl
     * Uses PostgreSQL tsvector with ranking
     */
    @Query(value = """
             SELECT s.*, ts_rank(s.search_vector, plainto_tsquery('english', :query)) as rank
             FROM services s
             WHERE s.enabled = true
               AND s.search_vector @@ plainto_tsquery('english', :query)
             ORDER BY rank DESC
             """, nativeQuery = true)
    List<ServiceEntity> search(@Param("query") String query);
}
