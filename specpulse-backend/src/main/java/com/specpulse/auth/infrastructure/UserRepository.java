package com.specpulse.auth.infrastructure;

import com.specpulse.auth.domain.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndEnabledTrueAndDeletedAtIsNull(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "roles")
    List<UserEntity> findAllByDeletedAtIsNull();

    Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);
}
