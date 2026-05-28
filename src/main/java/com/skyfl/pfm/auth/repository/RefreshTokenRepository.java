package com.skyfl.pfm.auth.repository;

import com.skyfl.pfm.auth.entity.RefreshToken;
import com.skyfl.pfm.user.entity.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    void deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
