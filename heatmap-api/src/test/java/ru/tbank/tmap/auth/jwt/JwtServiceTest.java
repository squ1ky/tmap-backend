package ru.tbank.tmap.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.auth.jwt.JwtService;
import ru.tbank.tmap.auth.jwt.JwtProperties;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRole;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String BASE64_SECRET = "bXktc3VwZXItc2VjcmV0LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=";
    private static final Duration ACCESS_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration REFRESH_EXPIRATION = Duration.ofDays(7);

    private JwtService jwtService;

    @Mock
    private User user;

    private final UUID testUserId = UUID.randomUUID();
    private final String testEmail = "test@tbank.ru";

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(BASE64_SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        jwtService = new JwtService(jwtProperties);
        jwtService.init();
    }

    @Test
    @DisplayName("Должен успешно генерировать Access Token")
    void generateAccessToken_whenValidUser_thenReturnToken() {
        mockUser();

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Должен возвращать true для валидного токена")
    void isValidAccessToken_whenTokenValid_thenReturnTrue() {
        mockUser();
        String validToken = jwtService.generateAccessToken(user);

        boolean isValid = jwtService.isValidAccessToken(validToken);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Должен возвращать false для невалидного или подделанного токена")
    void isValidAccessToken_whenTokenInvalid_thenReturnFalse() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidPayload.invalidSignature";

        boolean isValid = jwtService.isValidAccessToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Должен корректно извлекать Email из токена")
    void extractEmail_whenValidToken_thenReturnEmail() {
        mockUser();
        String token = jwtService.generateAccessToken(user);

        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Должен корректно извлекать UserId из токена")
    void extractUserId_whenValidToken_thenReturnUserId() {
        mockUser();
        String token = jwtService.generateAccessToken(user);

        UUID extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("Должен корректно извлекать Role из токена")
    void extractRole_whenValidToken_thenReturnRole() {
        mockUser();
        String token = jwtService.generateAccessToken(user);

        String extractedRole = jwtService.extractRole(token);

        assertThat(extractedRole).isEqualTo(UserRole.USER.name());
    }

    private void mockUser() {
        given(user.getEmail()).willReturn(testEmail);
        given(user.getId()).willReturn(testUserId);
        given(user.getRole()).willReturn(UserRole.USER);
    }
}
