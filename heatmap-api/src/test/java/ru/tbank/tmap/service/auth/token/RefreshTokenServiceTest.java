package ru.tbank.tmap.service.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.config.security.jwt.JwtProperties;
import ru.tbank.tmap.domain.user.RefreshToken;
import ru.tbank.tmap.domain.user.User;
import ru.tbank.tmap.repository.jpa.RefreshTokenRepository;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration REFRESH_EXPIRATION = Duration.ofDays(7);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private User user;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("secret", Duration.ofMinutes(15), REFRESH_EXPIRATION);

        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                tokenHasher,
                jwtProperties
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
}
