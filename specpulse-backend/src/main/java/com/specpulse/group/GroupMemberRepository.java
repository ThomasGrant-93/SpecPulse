package com.specpulse.group;

import com.specpulse.entity.GroupMember;
import com.specpulse.entity.ServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /**
     * Найти все членства в группе
     */
    List<GroupMember> findByGroupIdOrderByAddedAtDesc(Long groupId);

    /**
     * Проверить, состоит ли сервис в группе
     */
    boolean existsByGroupIdAndServiceId(Long groupId, Long serviceId);

    /**
     * Найти членство по группе и сервису
     */
    Optional<GroupMember> findByGroupIdAndServiceId(Long groupId, Long serviceId);

    /**
     * Удалить сервис из группы
     */
    @Modifying
    void deleteByGroupIdAndServiceId(Long groupId, Long serviceId);

    /**
     * Удалить все членства в группе
     */
    @Modifying
    void deleteByGroupId(Long groupId);

    /**
     * Получить группы, в которых состоит сервис
     */
    @Query("SELECT gm.group FROM GroupMember gm WHERE gm.service.id = :serviceId")
    List<ServiceGroup> findGroupsByServiceId(@Param("serviceId") Long serviceId);
}
