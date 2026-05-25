package ru.tbank.tmap.profile.presentation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.auth.application.port.TokenIssuer;
import ru.tbank.tmap.infrastructure.security.TestSecurityConfig;
import ru.tbank.tmap.profile.application.ProfileService;
import ru.tbank.tmap.profile.presentation.mapper.ProfileMapper;

@WebMvcTest(ProfileController.class)
@Import(TestSecurityConfig.class)
class ProfileSecurityTest {

    private static final String PROFILE_URL = "/api/v1/profile";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private ProfileMapper profileMapper;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TokenIssuer tokenIssuer;

    @Test
    void getMyProfile_whenTokenUserNotFound_thenReturnUnauthorized() throws Exception {
        given(tokenIssuer.isValidAccessToken("valid-token")).willReturn(true);
        given(tokenIssuer.extractEmail("valid-token")).willReturn("missing@tmap.local");
        given(userDetailsService.loadUserByUsername(anyString()))
                .willThrow(new UsernameNotFoundException("User not found: missing@tmap.local"));

        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(profileService);
    }
}
