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
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.tbank.tmap.auth.application.service.RefreshTokenService;
import ru.tbank.tmap.auth.domain.exception.InvalidCredentialsException;
import ru.tbank.tmap.profile.application.query.ProfileLoyaltyHistoryProjection;
import ru.tbank.tmap.profile.application.query.ProfileUsedPromoProjection;
import ru.tbank.tmap.profile.domain.repository.ProfileRepository;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.user.domain.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private ProfileLoyaltyHistoryProjection loyaltyHistoryProjection;

    @Mock
    private ProfileUsedPromoProjection usedPromoProjection;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userAccountFacade,
                profileRepository,
                passwordEncoder,
                refreshTokenService
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
    void changePassword_whenCurrentPasswordInvalid_thenThrowUnauthorized() {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .currentPassword("wrong-pass")
                .newPassword("NewPass123!");

        given(userAccountFacade.findById(USER_ID)).willReturn(Optional.of(testUserView()));
        given(passwordEncoder.matches("wrong-pass", CURRENT_HASH)).willReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(USER_ID, request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void changePassword_whenNewPasswordMatchesCurrent_thenThrowBadRequest() {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .currentPassword("Echak123!")
                .newPassword("NewPass123!");
        given(userAccountFacade.findById(USER_ID)).willReturn(Optional.of(testUserView()));
        given(passwordEncoder.matches("Echak123!", CURRENT_HASH)).willReturn(true);
        given(passwordEncoder.matches("NewPass123!", CURRENT_HASH)).willReturn(true);

        assertThatThrownBy(() -> profileService.changePassword(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New password must be different from current password");
    }

    @Test
    void changePassword_whenRequestValid_thenUpdateHashAndRevokeTokens() {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .currentPassword("Echak123!")
                .newPassword("NewPass123!");

        given(userAccountFacade.findById(USER_ID)).willReturn(Optional.of(testUserView()));
        given(passwordEncoder.matches("Echak123!", CURRENT_HASH)).willReturn(true);
        given(passwordEncoder.matches("NewPass123!", CURRENT_HASH)).willReturn(false);
        given(passwordEncoder.encode("NewPass123!")).willReturn(NEW_HASH);

        profileService.changePassword(USER_ID, request);

        verify(userAccountFacade).updatePasswordHash(USER_ID, NEW_HASH);
        verify(refreshTokenService).revokeAllForUser(USER_ID);
    }

    @Test
    void getLoyaltyHistory_returnsProjectionPage() {
        given(loyaltyHistoryProjection.venueName()).willReturn("Cafe One");
        given(profileRepository.findLoyaltyHistoryByUserId(eq(USER_ID), any()))
                .willReturn(new PageImpl<>(List.of(loyaltyHistoryProjection)));

        final Page<ProfileLoyaltyHistoryProjection> response = profileService.getLoyaltyHistory(USER_ID, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).venueName()).isEqualTo("Cafe One");
    }

    @Test
    void getUsedPromosHistory_returnsProjectionPage() {
        given(usedPromoProjection.description()).willReturn("Скидка 15% на капучино");
        given(profileRepository.findUsedPromosByUserId(eq(USER_ID), any()))
                .willReturn(new PageImpl<>(List.of(usedPromoProjection)));

        final Page<ProfileUsedPromoProjection> response = profileService.getUsedPromosHistory(USER_ID, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).description()).isEqualTo("Скидка 15% на капучино");
    }

    private UserView testUserView() {
        return new UserView(USER_ID, "user@tmap.local", CURRENT_HASH, "user", UserRole.USER, false);
    }
}
