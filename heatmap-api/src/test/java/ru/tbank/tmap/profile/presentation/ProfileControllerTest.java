package ru.tbank.tmap.profile.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.ChangePasswordRequest;
import org.openapitools.model.ProfileResponse;
import org.openapitools.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;
import ru.tbank.tmap.profile.application.ProfileService;
import ru.tbank.tmap.profile.application.exception.ProfilePasswordValidationException;
import ru.tbank.tmap.profile.presentation.mapper.ProfileMapper;
import ru.tbank.tmap.test.security.TestSecurityConfig;
import ru.tbank.tmap.user.api.UserView;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import(TestSecurityConfig.class)
@WithMockUser
class ProfileControllerTest {

    private static final String PROFILE_URL = "/api/v1/profile";
    private static final String PASSWORD_URL = "/api/v1/profile/password";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private ProfileMapper profileMapper;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getMyProfile_whenAuthenticated_thenReturnProfile() throws Exception {
        final UserView profileView = new UserView(
                USER_ID,
                "user@tmap.local",
                "ignored",
                "user",
                ru.tbank.tmap.user.domain.UserRole.USER,
                false
        );
        final ProfileResponse profileResponse = new ProfileResponse()
                .id(USER_ID)
                .email("user@tmap.local")
                .nickname("user")
                .role(UserRole.USER);
        given(profileService.getProfile(USER_ID)).willReturn(profileView);
        given(profileMapper.toResponse(profileView)).willReturn(profileResponse);

        mockMvc.perform(get(PROFILE_URL).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("user@tmap.local"))
                .andExpect(jsonPath("$.nickname").value("user"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(profileService).getProfile(USER_ID);
    }

    @Test
    void getMyProfile_whenAnonymous_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(get(PROFILE_URL).with(anonymous()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(profileService);
    }

    @Test
    void changeMyPassword_whenRequestValid_thenReturnNoContent() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .currentPassword("Echak123!")
                .newPassword("NewPass123!");

        mockMvc.perform(patch(PASSWORD_URL)
                        .with(csrf())
                        .with(user(principal()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(profileService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
    }

    @Test
    void changeMyPassword_whenNewPasswordMissing_thenReturnBadRequest() throws Exception {
        final String request = """
                {
                  "currentPassword": "Echak123!"
                }
                """;

        mockMvc.perform(patch(PASSWORD_URL)
                        .with(csrf())
                        .with(user(principal()))
                        .contentType("application/json")
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(profileService);
    }

    @Test
    void changeMyPassword_whenNewPasswordMatchesCurrent_thenReturnBadRequest() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .currentPassword("Echak123!")
                .newPassword("NewPass123!");
        willThrow(new ProfilePasswordValidationException())
                .given(profileService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));

        mockMvc.perform(patch(PASSWORD_URL)
                        .with(csrf())
                        .with(user(principal()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private CustomUserDetails principal() {
        return new CustomUserDetails(
                USER_ID,
                "user@tmap.local",
                "ignored",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
