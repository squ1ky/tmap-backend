package ru.tbank.tmap.service.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.config.security.jwt.JwtProperties;
import ru.tbank.tmap.domain.user.RefreshToken;
import ru.tbank.tmap.domain.user.User;
import ru.tbank.tmap.dto.auth.AuthResult;
import ru.tbank.tmap.exception.auth.InvalidRefreshTokenException;
import ru.tbank.tmap.repository.jpa.RefreshTokenRepository;
import ru.tbank.tmap.service.auth.jwt.JwtService;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issue(final User user) {
        final String plainToken = generatePlainToken();
        final RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                tokenHasher.hash(plainToken),
                OffsetDateTime.now().plus(jwtProperties.refreshExpiration())
        );
        refreshTokenRepository.save(refreshToken);
        return plainToken;
    }

    @Transactional
    public AuthResult rotate(final String plainToken) {
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

        final User user = existing.getUser();
        final String newPlainRefreshToken = issue(user);
        final String accessToken = jwtService.generateAccessToken(user);

        return new AuthResult(
                user.getId(),
                user.getRole(),
                accessToken,
                newPlainRefreshToken
        );
    }

    public String generatePlainToken() {
        final byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
