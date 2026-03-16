package com.specpulse.group;

import com.specpulse.entity.ServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceGroupRepository extends JpaRepository<ServiceGroup, Long> {

    /**
     * Найти все корневые группы (без родителя)
     */
    List<ServiceGroup> findByParentGroupIsNullOrderBySortOrderAsc();

    /**
     * Найти все группы с указанным родителем
     */
    List<ServiceGroup> findByParentGroupIdOrderBySortOrderAsc(Long parentGroupId);

    /**
     * Найти группу по имени и родителю
     */
    Optional<ServiceGroup> findByNameAndParentGroup(String name, ServiceGroup parentGroup);

    /**
     * Найти группу по имени (для корневых групп)
     */
    Optional<ServiceGroup> findByNameAndParentGroupIsNull(String name);

    /**
     * Проверить существование группы с таким именем
     */
    boolean existsByNameAndParentGroup(String name, ServiceGroup parentGroup);

    /**
     * Получить количество сервисов в группе
     */
    @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.group.id = :groupId")
    Integer countServicesByGroupId(@Param("groupId") Long groupId);

    /**
     * Получить все группы с количеством сервисов
     */
    @Query("""
                SELECT g, COUNT(m) as serviceCount
                FROM ServiceGroup g
                LEFT JOIN GroupMember m ON m.group = g
                GROUP BY g
                ORDER BY g.sortOrder ASC
            """)
    List<Object[]> findAllWithServiceCount();
}
