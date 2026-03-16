package com.specpulse.registry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceSearchRepository extends JpaRepository<ServiceEntity, Long> {

    /**
     * Get search suggestions for autocomplete - names
     */
    @Query(value = """
            SELECT DISTINCT s.name
            FROM services s
            WHERE s.enabled = true
              AND s.name ILIKE :query%
            ORDER BY s.name
            LIMIT 10
            """, nativeQuery = true)
    List<String> suggestServiceNames(@Param("query") String query);

    /**
     * Get search suggestions from description
     */
    @Query(value = """
            SELECT DISTINCT s.name
            FROM services s
            WHERE s.enabled = true
              AND s.description IS NOT NULL
              AND s.description ILIKE :query%
            ORDER BY s.name
            LIMIT 5
            """, nativeQuery = true)
    List<String> suggestByDescription(@Param("query") String query);

    /**
     * Full-text search with ranking
     */
    @Query(value = """
            SELECT s.*, ts_rank(s.search_vector, plainto_tsquery('english', :query)) as rank
            FROM services s
            WHERE s.enabled = true
              AND s.search_vector @@ plainto_tsquery('english', :query)
            ORDER BY rank DESC
            LIMIT 50
            """, nativeQuery = true)
    List<ServiceEntity> search(@Param("query") String query);

    /**
     * Find all enabled services
     */
    List<ServiceEntity> findByEnabledTrue();

    /**
     * Find all services (including disabled for soft delete)
     */
    List<ServiceEntity> findAll();

    /**
     * Check if service name exists
     */
    boolean existsByName(String name);

    /**
     * Find service by name
     */
    java.util.Optional<ServiceEntity> findByName(String name);
}
