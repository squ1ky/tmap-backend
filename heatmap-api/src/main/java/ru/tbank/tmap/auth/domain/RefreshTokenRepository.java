package ru.tbank.tmap.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findById(UUID id);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    int revokeByTokenHashAndUserId(String tokenHash, UUID userId);

    void deleteById(UUID id);
}
