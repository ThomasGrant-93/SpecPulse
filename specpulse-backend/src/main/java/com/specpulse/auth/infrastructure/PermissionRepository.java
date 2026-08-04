package com.specpulse.auth.infrastructure;

import com.specpulse.auth.domain.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByNameAndEnabledTrueAndDeletedAtIsNull(String name);

    Optional<PermissionEntity> findByNameAndDeletedAtIsNull(String name);
}
