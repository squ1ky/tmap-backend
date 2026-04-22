package ru.tbank.tmap.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.tbank.tmap.domain.user.User;
import ru.tbank.tmap.domain.user.UserRole;
import ru.tbank.tmap.dto.auth.AuthResult;
import ru.tbank.tmap.exception.auth.EmailAlreadyExistsException;
import ru.tbank.tmap.exception.auth.InvalidCredentialsException;
import ru.tbank.tmap.repository.jpa.UserRepository;
import ru.tbank.tmap.service.auth.jwt.JwtService;
import ru.tbank.tmap.service.auth.token.RefreshTokenService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final String testEmail = "user@tbank.ru";
    private final String testPassword = "password123";
    private final String testNickname = "tbank_user";
    private final String mockAccessToken = "access-token.abc.123";
    private final String mockRefreshToken = "refresh-token-base64";

    @Test
    @DisplayName("Регистрация: Ошибка, если email уже занят")
    void register_whenEmailAlreadyExists_thenThrowEmailAlreadyExistsException() {
        given(userRepository.existsByEmail(testEmail)).willReturn(true);

        assertThatThrownBy(() -> authService.register(testEmail, testPassword, testNickname))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("Регистрация: Успешное создание пользователя и выдача токенов")
    void register_whenValidData_thenCreateUserAndReturnAuthResult() {
        String encodedPassword = "encoded_password_hash";

        given(userRepository.existsByEmail(testEmail)).willReturn(false);
        given(passwordEncoder.encode(testPassword)).willReturn(encodedPassword);

        given(refreshTokenService.issue(any(User.class))).willReturn(mockRefreshToken);
        given(jwtService.generateAccessToken(any(User.class))).willReturn(mockAccessToken);

        AuthResult result = authService.register(testEmail, testPassword, testNickname);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(mockAccessToken);
        assertThat(result.plainRefreshToken()).isEqualTo(mockRefreshToken);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo(testEmail);
        assertThat(savedUser.getNickname()).isEqualTo(testNickname);
        assertThat(savedUser.getPasswordHash()).isEqualTo(encodedPassword);
    }

    @Test
    @DisplayName("Логин: Ошибка, если пользователь не найден")
    void login_whenUserNotFound_thenThrowInvalidCredentialsException() {
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(testEmail, testPassword))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("Логин: Ошибка, если пользователь заблокирован")
    void login_whenUserIsBlocked_thenThrowInvalidCredentialsException() {
        User mockUser = mock(User.class);
        given(mockUser.isBlocked()).willReturn(true);

        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.login(testEmail, testPassword))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("Логин: Ошибка, если пароль неверный")
    void login_whenPasswordIncorrect_thenThrowInvalidCredentialsException() {
        String savedHash = "saved_hash";
        User mockUser = mock(User.class);
        given(mockUser.isBlocked()).willReturn(false);
        given(mockUser.getPasswordHash()).willReturn(savedHash);

        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches(testPassword, savedHash)).willReturn(false);

        assertThatThrownBy(() -> authService.login(testEmail, testPassword))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("Логин: Успешная аутентификация и выдача токенов")
    void login_whenValidCredentials_thenReturnAuthResult() {
        String savedHash = "saved_hash";
        UUID userId = UUID.randomUUID();

        User mockUser = mock(User.class);
        given(mockUser.isBlocked()).willReturn(false);
        given(mockUser.getPasswordHash()).willReturn(savedHash);
        given(mockUser.getId()).willReturn(userId);
        given(mockUser.getRole()).willReturn(UserRole.USER);

        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches(testPassword, savedHash)).willReturn(true);

        given(refreshTokenService.issue(mockUser)).willReturn(mockRefreshToken);
        given(jwtService.generateAccessToken(mockUser)).willReturn(mockAccessToken);

        AuthResult result = authService.login(testEmail, testPassword);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.role()).isEqualTo(UserRole.USER);
        assertThat(result.accessToken()).isEqualTo(mockAccessToken);
        assertThat(result.plainRefreshToken()).isEqualTo(mockRefreshToken);
    }
}
