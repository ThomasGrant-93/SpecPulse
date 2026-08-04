package com.specpulse.auth.infrastructure;

import com.specpulse.auth.domain.RoleEntity;
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
