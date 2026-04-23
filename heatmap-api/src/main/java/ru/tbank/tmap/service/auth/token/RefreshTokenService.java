package ru.tbank.tmap.service.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.config.security.jwt.JwtProperties;
import ru.tbank.tmap.domain.user.RefreshToken;
import ru.tbank.tmap.domain.user.User;
import ru.tbank.tmap.repository.jpa.RefreshTokenRepository;

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

    public String generatePlainToken() {
        final byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
