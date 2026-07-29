package com.specpulse.auth.repository;

import com.specpulse.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndEnabledTrueAndDeletedAtIsNull(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);
}
