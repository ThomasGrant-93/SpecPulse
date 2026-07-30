package com.specpulse.auth.repository;

import com.specpulse.auth.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByNameAndEnabledTrueAndDeletedAtIsNull(String name);

    Optional<PermissionEntity> findByNameAndDeletedAtIsNull(String name);
}
