package ru.tbank.tmap.auth.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.auth.application.port.RefreshTokenHasher;
import ru.tbank.tmap.auth.domain.RefreshToken;
import ru.tbank.tmap.auth.domain.RefreshTokenRepository;
import ru.tbank.tmap.auth.infrastructure.security.JwtProperties;
import ru.tbank.tmap.auth.domain.exception.InvalidRefreshTokenException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration REFRESH_EXPIRATION = Duration.ofDays(7);
    private static final String PLAIN_TOKEN = "plain-token-value";
    private static final String TOKEN_HASH = "hashed-token-123";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenHasher tokenHasher;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private RefreshTokenService refreshTokenService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("secret", Duration.ofMinutes(15), REFRESH_EXPIRATION);

        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                tokenHasher,
                jwtProperties
        );

        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("issue: должен выдавать refresh токен, хэшировать его и сохранять в БД")
    void issue_whenValidUserId_thenSaveAndReturnPlainToken() {
        given(tokenHasher.hash(anyString())).willReturn(TOKEN_HASH);

        final String plainToken = refreshTokenService.issue(userId);

        assertThat(plainToken).isNotBlank();

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        final RefreshToken savedToken = refreshTokenCaptor.getValue();

        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getUserId()).isEqualTo(userId);
        assertThat(savedToken.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(savedToken.getExpiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(savedToken.getExpiresAt()).isBeforeOrEqualTo(OffsetDateTime.now().plusDays(7).plusMinutes(1));
    }

    @Test
    @DisplayName("issue: каждый вызов должен возвращать уникальный URL-safe Base64 токен")
    void issue_whenCalledMultipleTimes_thenReturnUniqueTokens() {
        given(tokenHasher.hash(anyString())).willReturn(TOKEN_HASH);

        final String token1 = refreshTokenService.issue(userId);
        final String token2 = refreshTokenService.issue(userId);

        assertThat(token1)
                .isNotBlank()
                .hasSize(43)
                .matches("^[a-zA-Z0-9_-]+$")
                .isNotEqualTo(token2);
    }

    @Test
    @DisplayName("validateAndRevoke: должен пометить токен отозванным и вернуть userId")
    void validateAndRevoke_whenTokenValid_thenRevokeAndReturnUserId() {
        final RefreshToken existing = new RefreshToken(
                UUID.randomUUID(),
                userId,
                TOKEN_HASH,
                OffsetDateTime.now().plusDays(1)
        );
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(existing));

        final UUID result = refreshTokenService.validateAndRevoke(PLAIN_TOKEN);

        assertThat(result).isEqualTo(userId);
        assertThat(existing.isRevoked()).isTrue();

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue()).isSameAs(existing);
        assertThat(refreshTokenCaptor.getValue().isRevoked()).isTrue();
    }

    @Test
    @DisplayName("validateAndRevoke: должен бросить исключение, если токен пустой")
    void validateAndRevoke_whenBlankToken_thenThrowInvalidRefreshToken() {
        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke("   "))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("missing");
        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke(null))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("missing");

        verify(tokenHasher, never()).hash(anyString());
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateAndRevoke: должен бросить исключение, если токен не найден")
    void validateAndRevoke_whenTokenNotFound_thenThrowInvalidRefreshToken() {
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke(PLAIN_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("not found");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateAndRevoke: должен бросить исключение, если токен уже отозван")
    void validateAndRevoke_whenTokenRevoked_thenThrowInvalidRefreshToken() {
        final RefreshToken revoked = new RefreshToken(
                UUID.randomUUID(),
                userId,
                TOKEN_HASH,
                OffsetDateTime.now().plusDays(1)
        );
        revoked.setRevoked(true);
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke(PLAIN_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateAndRevoke: должен бросить исключение, если токен истёк")
    void validateAndRevoke_whenTokenExpired_thenThrowInvalidRefreshToken() {
        final RefreshToken expired = new RefreshToken(
                UUID.randomUUID(),
                userId,
                TOKEN_HASH,
                OffsetDateTime.now().minusMinutes(1)
        );
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke(PLAIN_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeSpecificToken: должен хэшировать токен и отзывать конкретную сессию в БД")
    void revokeSpecificToken_whenValidToken_thenDelegateToRepository() {
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.revokeByTokenHashAndUserId(TOKEN_HASH, userId)).willReturn(1);

        refreshTokenService.revokeSpecificToken(userId, PLAIN_TOKEN);

        verify(tokenHasher).hash(PLAIN_TOKEN);
        verify(refreshTokenRepository).revokeByTokenHashAndUserId(TOKEN_HASH, userId);
    }

    @Test
    @DisplayName("revokeSpecificToken: должен ничего не делать, если токен пустой")
    void revokeSpecificToken_whenBlankToken_thenDoNothing() {
        refreshTokenService.revokeSpecificToken(userId, "   ");
        refreshTokenService.revokeSpecificToken(userId, null);

        verify(tokenHasher, never()).hash(anyString());
        verify(refreshTokenRepository, never()).revokeByTokenHashAndUserId(anyString(), any(UUID.class));
    }
}
