package ru.tbank.tmap.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.auth.application.port.RefreshTokenHasher;
import ru.tbank.tmap.auth.domain.RefreshToken;
import ru.tbank.tmap.auth.domain.RefreshTokenRepository;
import ru.tbank.tmap.auth.infrastructure.security.JwtProperties;
import ru.tbank.tmap.auth.domain.exception.InvalidRefreshTokenException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher tokenHasher;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issue(final UUID userId) {
        final String plainToken = generatePlainToken();
        final RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                userId,
                tokenHasher.hash(plainToken),
                OffsetDateTime.now().plus(jwtProperties.refreshExpiration())
        );
        refreshTokenRepository.save(refreshToken);
        return plainToken;
    }

    /**
     * Validates a plain refresh token, revokes it, and returns the owner's user id.
     * Throws {@link InvalidRefreshTokenException} if the token is missing, unknown,
     * already revoked, or expired.
     */
    @Transactional
    public UUID validateAndRevoke(final String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        final String hash = tokenHasher.hash(plainToken);
        final RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (existing.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token is revoked");
        }
        if (existing.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token is expired");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return existing.getUserId();
    }

    @Transactional
    public void revokeSpecificToken(final UUID userId, final String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return;
        }

        final String hash = tokenHasher.hash(plainToken);
        final int count = refreshTokenRepository.revokeByTokenHashAndUserId(hash, userId);

        if (count > 0) {
            log.info("Revoked specific refresh token for user {}", userId);
        }
    }

    public String generatePlainToken() {
        final byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
