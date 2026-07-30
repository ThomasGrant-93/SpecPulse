package com.specpulse.auth.repository;

import com.specpulse.auth.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByNameAndEnabledTrueAndDeletedAtIsNull(String name);

    Optional<RoleEntity> findByNameAndDeletedAtIsNull(String name);

    List<RoleEntity> findAllByEnabledTrueAndDeletedAtIsNull();

    List<RoleEntity> findByNameInAndEnabledTrueAndDeletedAtIsNull(Collection<String> names);
}
