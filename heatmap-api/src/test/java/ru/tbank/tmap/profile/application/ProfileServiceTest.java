package ru.tbank.tmap.profile.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.ChangePasswordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.tbank.tmap.auth.api.AuthAccountFacade;
import ru.tbank.tmap.loyalty.api.LoyaltyProfileFacade;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.user.domain.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333331");
    private static final UUID VERIFICATION_ID = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final String CURRENT_HASH = "current-hash";
    private static final String NEW_HASH = "new-hash";

    @Mock
    private UserAccountFacade userAccountFacade;

    @Mock
    private LoyaltyProfileFacade loyaltyProfileFacade;

    @Mock
    private AuthAccountFacade authAccountFacade;

    @Mock
    private LoyaltyHistoryProjection loyaltyHistoryProjection;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userAccountFacade,
                authAccountFacade,
                loyaltyProfileFacade
        );
    }

    @Test
    void getProfile_returnsCurrentUserData() {
        given(userAccountFacade.findById(USER_ID)).willReturn(Optional.of(new UserView(
                USER_ID,
                "user@tmap.local",
                CURRENT_HASH,
                "user",
                UserRole.USER,
                false
        )));

        final UserView response = profileService.getProfile(USER_ID);

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo("user@tmap.local");
        assertThat(response.nickname()).isEqualTo("user");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void changePassword_whenRequestValid_thenDelegateToAuthFacade() {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .currentPassword("Echak123!")
                .newPassword("NewPass123!");

        profileService.changePassword(USER_ID, request);

        verify(authAccountFacade).changePassword(USER_ID, "Echak123!", "NewPass123!");
    }

    @Test
    void getLoyaltyHistory_returnsProjectionPage() {
        given(loyaltyHistoryProjection.venueName()).willReturn("Cafe One");
        given(loyaltyProfileFacade.findUserLoyaltyHistory(eq(USER_ID), any()))
                .willReturn(new PageImpl<>(List.of(loyaltyHistoryProjection)));

        final Page<LoyaltyHistoryProjection> response = profileService.getLoyaltyHistory(USER_ID, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).venueName()).isEqualTo("Cafe One");
    }
}
