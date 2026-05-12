package ru.tbank.tmap.auth.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.tbank.tmap.auth.application.AuthResult;
import ru.tbank.tmap.auth.application.exception.PasswordChangeValidationException;
import ru.tbank.tmap.auth.application.port.TokenIssuer;
import ru.tbank.tmap.auth.application.port.UserAccountPort;
import ru.tbank.tmap.auth.domain.exception.InvalidRefreshTokenException;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.user.domain.UserRole;
import ru.tbank.tmap.user.api.exception.EmailAlreadyExistsException;
import ru.tbank.tmap.auth.domain.exception.InvalidCredentialsException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountPort userAccountPort;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "user@tbank.ru";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded_password_hash";
    private static final String NICKNAME = "tbank_user";
    private static final String ACCESS_TOKEN = "access-token.abc.123";
    private static final String REFRESH_TOKEN = "refresh-token-base64";

    private UserView userView(UUID id, boolean blocked) {
        return new UserView(id, EMAIL, ENCODED_PASSWORD, NICKNAME, UserRole.USER, blocked);
    }

    @Test
    @DisplayName("register: успешное создание пользователя и выдача токенов")
    void register_whenValidData_thenCreateUserAndReturnAuthResult() {
        final UUID userId = UUID.randomUUID();
        final UserView user = userView(userId, false);

        given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userAccountPort.register(EMAIL, ENCODED_PASSWORD, NICKNAME)).willReturn(user);
        given(refreshTokenService.issue(userId)).willReturn(REFRESH_TOKEN);
        given(tokenIssuer.generateAccessToken(userId, EMAIL, UserRole.USER.name())).willReturn(ACCESS_TOKEN);

        final AuthResult result = authService.register(EMAIL, PASSWORD, NICKNAME);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.nickname()).isEqualTo(NICKNAME);
        assertThat(result.role()).isEqualTo(UserRole.USER.name());
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.plainRefreshToken()).isEqualTo(REFRESH_TOKEN);

        verify(userAccountPort).register(EMAIL, ENCODED_PASSWORD, NICKNAME);
    }

    @Test
    @DisplayName("register: пробрасывает EmailAlreadyExistsException из user-модуля")
    void register_whenEmailAlreadyExists_thenPropagateException() {
        given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);
        willThrow(new EmailAlreadyExistsException(EMAIL))
                .given(userAccountPort).register(EMAIL, ENCODED_PASSWORD, NICKNAME);

        assertThatThrownBy(() -> authService.register(EMAIL, PASSWORD, NICKNAME))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(tokenIssuer, refreshTokenService);
    }

    @Test
    @DisplayName("login: ошибка, если пользователь не найден")
    void login_whenUserNotFound_thenThrowInvalidCredentialsException() {
        given(userAccountPort.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, tokenIssuer, refreshTokenService);
    }

    @Test
    @DisplayName("login: ошибка, если пользователь заблокирован")
    void login_whenUserIsBlocked_thenThrowInvalidCredentialsException() {
        final UserView blocked = userView(UUID.randomUUID(), true);
        given(userAccountPort.findByEmail(EMAIL)).willReturn(Optional.of(blocked));

        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, tokenIssuer, refreshTokenService);
    }

    @Test
    @DisplayName("login: ошибка, если пароль неверный")
    void login_whenPasswordIncorrect_thenThrowInvalidCredentialsException() {
        final UserView user = userView(UUID.randomUUID(), false);
        given(userAccountPort.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenIssuer, refreshTokenService);
    }

    @Test
    @DisplayName("login: успешная аутентификация и выдача токенов")
    void login_whenValidCredentials_thenReturnAuthResult() {
        UUID userId = UUID.randomUUID();
        final UserView user = userView(userId, false);

        given(userAccountPort.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(refreshTokenService.issue(userId)).willReturn(REFRESH_TOKEN);
        given(tokenIssuer.generateAccessToken(userId, EMAIL, UserRole.USER.name())).willReturn(ACCESS_TOKEN);

        AuthResult result = authService.login(EMAIL, PASSWORD);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.nickname()).isEqualTo(NICKNAME);
        assertThat(result.role()).isEqualTo(UserRole.USER.name());
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.plainRefreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh: валидный токен -> новая пара access/refresh")
    void refresh_whenTokenValid_thenIssueNewPair() {
        final UUID userId = UUID.randomUUID();
        final UserView user = userView(userId, false);
        final String plainRefresh = "old-refresh";
        final String newRefresh = "new-refresh";

        given(refreshTokenService.validateAndRevoke(plainRefresh)).willReturn(userId);
        given(userAccountPort.findById(userId)).willReturn(Optional.of(user));
        given(refreshTokenService.issue(userId)).willReturn(newRefresh);
        given(tokenIssuer.generateAccessToken(userId, EMAIL, UserRole.USER.name()))
                .willReturn(ACCESS_TOKEN);

        final AuthResult result = authService.refresh(plainRefresh);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.nickname()).isEqualTo(NICKNAME);
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.plainRefreshToken()).isEqualTo(newRefresh);
    }

    @Test
    @DisplayName("refresh: невалидный токен ->  проброс InvalidRefreshTokenException")
    void refresh_whenTokenInvalid_thenPropagateException() {
        final String plainRefresh = "bad-token";
        willThrow(new InvalidRefreshTokenException("expired"))
                .given(refreshTokenService).validateAndRevoke(plainRefresh);

        assertThatThrownBy(() -> authService.refresh(plainRefresh))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(userAccountPort, tokenIssuer);
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    @DisplayName("refresh: пользователь по userId не найден дает InvalidCredentialsException")
    void refresh_whenUserNotFound_thenThrowInvalidCredentials() {
        final UUID userId = UUID.randomUUID();
        final String plainRefresh = "valid-but-orphan";

        given(refreshTokenService.validateAndRevoke(plainRefresh)).willReturn(userId);
        given(userAccountPort.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(plainRefresh))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenIssuer);
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    @DisplayName("logout: делегирует отзыв конкретного токена в RefreshTokenService")
    void logout_thenDelegateToRefreshTokenService() {
        final UUID userId = UUID.randomUUID();
        final String plainRefresh = "to-revoke";

        authService.logout(userId, plainRefresh);

        verify(refreshTokenService).revokeSpecificToken(userId, plainRefresh);
        verifyNoInteractions(userAccountPort, tokenIssuer, passwordEncoder);
    }

    @Test
    @DisplayName("changePassword: ошибка, если текущий пароль неверный")
    void changePassword_whenCurrentPasswordInvalid_thenThrowInvalidCredentials() {
        final UUID userId = UUID.randomUUID();
        final UserView user = userView(userId, false);

        given(userAccountPort.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-pass", ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword(userId, "wrong-pass", "new-pass"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userAccountPort, never()).updatePasswordHash(any(), org.mockito.ArgumentMatchers.anyString());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("changePassword: ошибка, если новый пароль совпадает с текущим")
    void changePassword_whenNewPasswordMatchesCurrent_thenThrowValidation() {
        final UUID userId = UUID.randomUUID();
        final UserView user = userView(userId, false);

        given(userAccountPort.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("current-pass", ENCODED_PASSWORD)).willReturn(true);
        given(passwordEncoder.matches("new-pass", ENCODED_PASSWORD)).willReturn(true);

        assertThatThrownBy(() -> authService.changePassword(userId, "current-pass", "new-pass"))
                .isInstanceOf(PasswordChangeValidationException.class);

        verify(userAccountPort, never()).updatePasswordHash(any(), org.mockito.ArgumentMatchers.anyString());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("changePassword: обновляет хеш и отзывает все refresh токены")
    void changePassword_whenValidRequest_thenUpdateHashAndRevokeTokens() {
        final UUID userId = UUID.randomUUID();
        final UserView user = userView(userId, false);

        given(userAccountPort.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("current-pass", ENCODED_PASSWORD)).willReturn(true);
        given(passwordEncoder.matches("new-pass", ENCODED_PASSWORD)).willReturn(false);
        given(passwordEncoder.encode("new-pass")).willReturn("new-hash");

        authService.changePassword(userId, "current-pass", "new-pass");

        verify(userAccountPort).updatePasswordHash(userId, "new-hash");
        verify(refreshTokenService).revokeAllForUser(userId);
    }

    private static UUID any() {
        return org.mockito.ArgumentMatchers.any(UUID.class);
    }
}
