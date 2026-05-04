package ru.tbank.tmap.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.user.domain.UserRole;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class JjwtTokenIssuerTest {

    private static final String BASE64_SECRET = "bXktc3VwZXItc2VjcmV0LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=";
    private static final Duration ACCESS_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration REFRESH_EXPIRATION = Duration.ofDays(7);

    private static final String EMAIL  = "test@tbank.ru";
    private static final String ROLE = UserRole.USER.name();

    private JjwtTokenIssuer tokenIssuer;
    private UUID userId;

    @BeforeEach
    void setUp() {
        final JwtProperties jwtProperties = new JwtProperties(BASE64_SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        tokenIssuer = new JjwtTokenIssuer(jwtProperties);
        tokenIssuer.init();

        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("generateAccessToken: возвращает корректно структурированный JWT")
    void generateAccessToken_whenValidUser_thenReturnToken() {
        final String token = tokenIssuer.generateAccessToken(userId, EMAIL, ROLE);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("isValidAccessToken: возращает true для валидного токена")
    void isValidAccessToken_whenTokenValid_thenReturnTrue() {
        final String validToken = tokenIssuer.generateAccessToken(userId, EMAIL, ROLE);

        assertThat(tokenIssuer.isValidAccessToken(validToken)).isTrue();
    }

    @Test
    @DisplayName("isValidAccessToken: возращает false для невалидного или подделанного токена")
    void isValidAccessToken_whenTokenInvalid_thenReturnFalse() {
        final String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidPayload.invalidSignature";

        assertThat(tokenIssuer.isValidAccessToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("isValidAccessToken: возвращает false для пустой/невалидной строки")
    void isValidAccessToken_whenTokenBlank_thenReturnFalse() {
        assertThat(tokenIssuer.isValidAccessToken("")).isFalse();
        assertThat(tokenIssuer.isValidAccessToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("extractEmail: должен корректно извлекать email из токена")
    void extractEmail_whenValidToken_thenReturnEmail() {
        final String token = tokenIssuer.generateAccessToken(userId, EMAIL, ROLE);

        assertThat(tokenIssuer.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("extractUserId: должен корректно извлекать UserId из токена")
    void extractUserId_whenValidToken_thenReturnUserId() {
        final String token = tokenIssuer.generateAccessToken(userId, EMAIL, ROLE);

        assertThat(tokenIssuer.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("extractRole: должен корректно извлекать Role из токена")
    void extractRole_whenValidToken_thenReturnRole() {
        final String token = tokenIssuer.generateAccessToken(userId, EMAIL, ROLE);

        assertThat(tokenIssuer.extractRole(token)).isEqualTo(ROLE);
    }
}
