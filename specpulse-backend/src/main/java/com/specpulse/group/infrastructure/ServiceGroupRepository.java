package com.specpulse.group.infrastructure;

import com.specpulse.group.domain.ServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceGroupRepository extends JpaRepository<ServiceGroup, Long> {

    /**
     * Find all root groups (without a parent)
     */
    List<ServiceGroup> findByParentGroupIsNullOrderBySortOrderAsc();

    /**
     * Find all groups with the specified parent
     */
    List<ServiceGroup> findByParentGroupIdOrderBySortOrderAsc(Long parentGroupId);

    /**
     * Find a group by name and parent
     */
    Optional<ServiceGroup> findByNameAndParentGroup(String name, ServiceGroup parentGroup);

    /**
     * Find a group by name (for root groups)
     */
    Optional<ServiceGroup> findByNameAndParentGroupIsNull(String name);

    /**
     * Check whether a group with this name exists
     */
    boolean existsByNameAndParentGroup(String name, ServiceGroup parentGroup);

    /**
     * Get the number of services in a group
     */
    @Query("SELECT COUNT(s) FROM com.specpulse.registry.domain.ServiceEntity s WHERE s.group.id = :groupId")
    Integer countServicesByGroupId(@Param("groupId") Long groupId);

    /**
     * Get all groups along with their service counts
     */
    @Query("""
                SELECT g, COUNT(s) as serviceCount
                FROM ServiceGroup g
                LEFT JOIN com.specpulse.registry.domain.ServiceEntity s ON s.group = g
                GROUP BY g
                ORDER BY g.sortOrder ASC
            """)
    List<Object[]> findAllWithServiceCount();
}
