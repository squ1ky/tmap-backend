package ru.tbank.tmap.auth.refresh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.auth.domain.RefreshToken;
import ru.tbank.tmap.auth.domain.RefreshTokenRepository;
import ru.tbank.tmap.auth.jwt.JwtProperties;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRole;
import ru.tbank.tmap.auth.AuthResult;
import ru.tbank.tmap.auth.domain.exception.InvalidRefreshTokenException;
import ru.tbank.tmap.auth.jwt.JwtService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration REFRESH_EXPIRATION = Duration.ofDays(7);
    private static final String PLAIN_TOKEN = "plain-token-value";
    private static final String TOKEN_HASH = "hashed-token-123";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenHasher tokenHasher;

    @Mock
    private JwtService jwtService;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("secret", Duration.ofMinutes(15), REFRESH_EXPIRATION);

        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                tokenHasher,
                jwtProperties,
                jwtService
        );

        user = new User(
                UUID.randomUUID(),
                "test@example.com",
                "password-hash",
                "nickname",
                UserRole.USER
        );
    }

    @Test
    @DisplayName("Должен выдавать refresh токен, хэшировать его и сохранять в БД")
    void issue_whenValidUser_thenSaveAndReturnPlainToken() {
        String expectedHash = "hashed-token-123";
        given(tokenHasher.hash(anyString())).willReturn(expectedHash);

        String plainToken = refreshTokenService.issue(user);

        assertThat(plainToken).isNotBlank();

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedToken = refreshTokenCaptor.getValue();

        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTokenHash()).isEqualTo(expectedHash);
        assertThat(savedToken.getExpiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(savedToken.getExpiresAt()).isBeforeOrEqualTo(OffsetDateTime.now().plusDays(7).plusMinutes(1));
    }

    @Test
    @DisplayName("Должен генерировать уникальный URL-safe Base64 токен корректной длины")
    void generatePlainToken_whenCalled_thenReturnValidBase64String() {
        String token1 = refreshTokenService.generatePlainToken();
        String token2 = refreshTokenService.generatePlainToken();

        assertThat(token1).isNotBlank();
        assertThat(token1).hasSize(43);
        assertThat(token1).matches("^[a-zA-Z0-9_-]+$");
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Должен отозвать старый токен и выпустить новую пару")
    void rotate_whenTokenValid_thenRevokeOldAndIssueNewPair() {
        final RefreshToken existing = new RefreshToken(
                UUID.randomUUID(),
                user,
                TOKEN_HASH,
                OffsetDateTime.now().plusDays(1)
        );
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(tokenHasher.hash(org.mockito.ArgumentMatchers
                .argThat(arg -> !PLAIN_TOKEN.equals(arg))))
                .willReturn("new-hash");
        given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(existing));
        given(jwtService.generateAccessToken(user)).willReturn(NEW_ACCESS_TOKEN);

        final AuthResult result = refreshTokenService.rotate(PLAIN_TOKEN);

        assertThat(existing.isRevoked()).isTrue();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.role()).isEqualTo(UserRole.USER);
        assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.plainRefreshToken()).isNotBlank().isNotEqualTo(PLAIN_TOKEN);

        verify(refreshTokenRepository, times(2)).save(refreshTokenCaptor.capture());
        final List<RefreshToken> saved = refreshTokenCaptor.getAllValues();

        final RefreshToken revokedSave = saved.get(0);
        assertThat(revokedSave).isSameAs(existing);
        assertThat(revokedSave.isRevoked()).isTrue();

        final RefreshToken newSave = saved.get(1);
        assertThat(newSave).isNotSameAs(existing);
        assertThat(newSave.getUser()).isEqualTo(user);
        assertThat(newSave.isRevoked()).isFalse();
        assertThat(newSave.getTokenHash()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("Должен бросить исключение, если токен уже отозван")
    void rotate_whenTokenRevoked_thenThrowInvalidRefreshToken() {
        final RefreshToken revoked = new RefreshToken(
                UUID.randomUUID(),
                user,
                TOKEN_HASH,
                OffsetDateTime.now().plusDays(1)
        );
        revoked.setRevoked(true);
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotate(PLAIN_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(user);
    }

    @Test
    @DisplayName("Должен бросить исключение, если токен не найден")
    void rotate_whenTokenNotFound_thenThrowInvalidRefreshToken() {
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate(PLAIN_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("not found");

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(user);
    }

    @Test
    @DisplayName("revokeSpecificToken: должен хэшировать токен и отзывать конкретную сессию в БД")
    void revokeSpecificToken_whenValidToken_thenDelegateToRepository() {
        final UUID userId = UUID.randomUUID();
        given(tokenHasher.hash(PLAIN_TOKEN)).willReturn(TOKEN_HASH);
        given(refreshTokenRepository.revokeByTokenHashAndUserId(TOKEN_HASH, userId)).willReturn(1);

        refreshTokenService.revokeSpecificToken(userId, PLAIN_TOKEN);

        verify(tokenHasher).hash(PLAIN_TOKEN);
        verify(refreshTokenRepository).revokeByTokenHashAndUserId(TOKEN_HASH, userId);
    }

    @Test
    @DisplayName("revokeSpecificToken: должен ничего не делать, если токен пустой")
    void revokeSpecificToken_whenBlankToken_thenDoNothing() {
        final UUID userId = UUID.randomUUID();

        refreshTokenService.revokeSpecificToken(userId, "   ");
        refreshTokenService.revokeSpecificToken(userId, null);

        verify(tokenHasher, never()).hash(anyString());
        verify(refreshTokenRepository, never()).revokeByTokenHashAndUserId(anyString(), any(UUID.class));
    }
}
