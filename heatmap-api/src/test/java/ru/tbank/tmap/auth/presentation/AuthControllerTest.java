package ru.tbank.tmap.auth.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.auth.application.AuthResult;
import ru.tbank.tmap.auth.application.service.AuthService;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;
import ru.tbank.tmap.auth.infrastructure.security.cookie.RefreshTokenCookieFactory;
import ru.tbank.tmap.test.security.TestSecurityConfig;
import ru.tbank.tmap.user.api.exception.EmailAlreadyExistsException;
import ru.tbank.tmap.auth.domain.exception.InvalidCredentialsException;
import ru.tbank.tmap.auth.domain.exception.InvalidRefreshTokenException;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        TestSecurityConfig.class,
        AuthControllerTest.TestBeans.class
})
@WithMockUser
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private static final String VALID_EMAIL = "kazan_guest@example.com";
    private static final String VALID_PASSWORD = "Echak123!";
    private static final String VALID_NICKNAME = "Tatarin116";
    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String REFRESH_TOKEN = "refresh-token-value";
    private static final UUID USER_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

    private AuthResult authResult;

    @BeforeEach
    void setUp() {
        authResult = new AuthResult(
                USER_ID,
                VALID_EMAIL,
                VALID_NICKNAME,
                "USER",
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        final ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN)
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .sameSite("Strict")
                .maxAge(604_800)
                .build();
        given(refreshTokenCookieFactory.create(REFRESH_TOKEN)).willReturn(cookie);
    }

    @Test
    void registerUser_whenRequestValid_thenReturnCreatedWithTokensAndCookie() throws Exception {
        given(authService.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME)).willReturn(authResult);
        final RegisterRequest request = new RegisterRequest()
                .email(VALID_EMAIL)
                .password(VALID_PASSWORD)
                .nickname(VALID_NICKNAME);

        mockMvc.perform(post(REGISTER_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.nickname").value(VALID_NICKNAME))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().secure(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().path(REFRESH_TOKEN_COOKIE_NAME, COOKIE_PATH))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN));

        verify(authService).register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);
    }

    @Test
    void registerUser_whenEmailAlreadyExists_thenReturnConflict() throws Exception {
        willThrow(new EmailAlreadyExistsException(VALID_EMAIL))
                .given(authService).register(eq(VALID_EMAIL), any(), any());
        final RegisterRequest request = new RegisterRequest()
                .email(VALID_EMAIL)
                .password(VALID_PASSWORD)
                .nickname(VALID_NICKNAME);

        mockMvc.perform(post(REGISTER_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void registerUser_whenEmailInvalid_thenReturnBadRequest() throws Exception {
        final RegisterRequest request = new RegisterRequest()
                .email("not-an-email")
                .password(VALID_PASSWORD)
                .nickname(VALID_NICKNAME);

        mockMvc.perform(post(REGISTER_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("email")));

        verifyNoInteractions(authService);
    }

    @Test
    void registerUser_whenPasswordTooShort_thenReturnBadRequest() throws Exception {
        final RegisterRequest request = new RegisterRequest()
                .email(VALID_EMAIL)
                .password("Ab1")
                .nickname(VALID_NICKNAME);

        mockMvc.perform(post(REGISTER_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(authService);
    }

    @Test
    void registerUser_whenNicknameTooShort_thenReturnBadRequest() throws Exception {
        final RegisterRequest request = new RegisterRequest()
                .email(VALID_EMAIL)
                .password(VALID_PASSWORD)
                .nickname("ab");

        mockMvc.perform(post(REGISTER_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(authService);
    }

    @Test
    void loginUser_whenCredentialsValid_thenReturnOkWithTokensAndCookie() throws Exception {
        given(authService.login(VALID_EMAIL, VALID_PASSWORD)).willReturn(authResult);
        final LoginRequest request = new LoginRequest()
                .email(VALID_EMAIL)
                .password(VALID_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.nickname").value(VALID_NICKNAME))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN));

        verify(authService).login(VALID_EMAIL, VALID_PASSWORD);
    }

    @Test
    void loginUser_whenCredentialsInvalid_thenReturnUnauthorized() throws Exception {
        willThrow(new InvalidCredentialsException())
                .given(authService).login(eq(VALID_EMAIL), any());
        final LoginRequest request = new LoginRequest()
                .email(VALID_EMAIL)
                .password("WrongPass1!");

        mockMvc.perform(post(LOGIN_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void loginUser_whenEmailMissing_thenReturnBadRequest() throws Exception {
        final LoginRequest request = new LoginRequest()
                .password(VALID_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(authService);
    }

    @Test
    void refreshAuthToken_whenTokenValid_thenReturnOkWithNewPair() throws Exception {
        final String oldRefreshToken = "old-refresh-token";
        given(authService.refresh(oldRefreshToken)).willReturn(authResult);

        mockMvc.perform(post(REFRESH_URL)
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.nickname").value(VALID_NICKNAME))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().path(REFRESH_TOKEN_COOKIE_NAME, COOKIE_PATH))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN));

        verify(authService).refresh(oldRefreshToken);
    }

    @Test
    void refreshAuthToken_whenTokenInvalid_thenReturnUnauthorized() throws Exception {
        final String badToken = "bad-refresh-token";
        willThrow(new InvalidRefreshTokenException("Refresh token is revoked"))
                .given(authService).refresh(badToken);

        mockMvc.perform(post(REFRESH_URL)
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, badToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void logoutUser_whenAuthenticated_thenReturnNoContentAndExpiredCookie() throws Exception {
        final ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .sameSite("Strict")
                .maxAge(0)
                .build();
        given(refreshTokenCookieFactory.createExpired()).willReturn(expiredCookie);

        final CustomUserDetails principal = new CustomUserDetails(
                USER_ID,
                VALID_EMAIL,
                "ignored",
                true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(post(LOGOUT_URL)
                        .with(csrf())
                        .with(user(principal))
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, ""))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0))
                .andExpect(cookie().path(REFRESH_TOKEN_COOKIE_NAME, COOKIE_PATH));

        verify(authService).logout(USER_ID, REFRESH_TOKEN);
        verify(refreshTokenCookieFactory).createExpired();
    }

    @Test
    void logoutUser_whenAnonymous_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(post(LOGOUT_URL)
                        .with(csrf())
                        .with(anonymous()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        RefreshTokenCookieFactory refreshTokenCookieFactory() {
            return org.mockito.Mockito.mock(RefreshTokenCookieFactory.class);
        }
    }
}
