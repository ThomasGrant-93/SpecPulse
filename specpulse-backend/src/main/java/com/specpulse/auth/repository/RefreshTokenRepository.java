package com.specpulse.auth.repository;

import com.specpulse.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, LocalDateTime now);
}
