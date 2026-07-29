package com.specpulse.auth.repository;

import com.specpulse.auth.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByNameAndEnabledTrueAndDeletedAtIsNull(String name);
}
