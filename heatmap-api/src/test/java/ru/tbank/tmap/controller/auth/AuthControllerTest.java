package ru.tbank.tmap.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.config.security.cookie.RefreshTokenCookieFactory;
import ru.tbank.tmap.domain.user.UserRole;
import ru.tbank.tmap.dto.auth.AuthResult;
import ru.tbank.tmap.exception.auth.EmailAlreadyExistsException;
import ru.tbank.tmap.exception.auth.InvalidCredentialsException;
import ru.tbank.tmap.service.auth.AuthService;
import ru.tbank.tmap.service.auth.jwt.JwtService;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestBeans.class)
@WithMockUser
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    private static final String VALID_EMAIL = "kazan_guest@example.com";
    private static final String VALID_PASSWORD = "Echak123!";
    private static final String VALID_NICKNAME = "Tatarin116";
    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String REFRESH_TOKEN = "refresh-token-value";
    private static final UUID USER_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

    private AuthResult authResult;

    @BeforeEach
    void setUp() {
        authResult = new AuthResult(USER_ID, UserRole.USER, ACCESS_TOKEN, REFRESH_TOKEN);

        final ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh")
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
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().secure(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().path(REFRESH_TOKEN_COOKIE_NAME, "/api/v1/auth/refresh"))
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

    @TestConfiguration
    static class TestBeans {

        @Bean
        RefreshTokenCookieFactory refreshTokenCookieFactory() {
            return org.mockito.Mockito.mock(RefreshTokenCookieFactory.class);
        }
    }
}