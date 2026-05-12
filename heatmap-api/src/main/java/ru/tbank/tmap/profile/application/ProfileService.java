package ru.tbank.tmap.profile.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.ChangePasswordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.auth.application.service.RefreshTokenService;
import ru.tbank.tmap.auth.domain.exception.InvalidCredentialsException;
import ru.tbank.tmap.profile.application.exception.ProfilePasswordValidationException;
import ru.tbank.tmap.profile.application.query.ProfileLoyaltyHistoryProjection;
import ru.tbank.tmap.profile.application.query.ProfileLoyaltyQr;
import ru.tbank.tmap.profile.application.query.ProfileUsedPromoProjection;
import ru.tbank.tmap.profile.domain.repository.ProfileRepository;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    // Temporary payload until the loyalty QR format is finalized and integrated end-to-end.
    private static final String LOYALTY_QR_STUB_PAYLOAD = "profile-loyalty-qr-stub";

    private final UserAccountFacade userAccountFacade;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserView getProfile(final UUID userId) {
        return userAccountFacade.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
    }

    @Transactional
    public void changePassword(final UUID userId, final ChangePasswordRequest request) {
        final UserView user = getProfile(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.passwordHash())) {
            throw new ProfilePasswordValidationException();
        }

        userAccountFacade.updatePasswordHash(userId, passwordEncoder.encode(request.getNewPassword()));
        refreshTokenService.revokeAllForUser(userId);
    }

    public ProfileLoyaltyQr getLoyaltyQr(final UUID userId) {
        getProfile(userId);
        return new ProfileLoyaltyQr(userId, LOYALTY_QR_STUB_PAYLOAD);
    }

    public Page<ProfileLoyaltyHistoryProjection> getLoyaltyHistory(final UUID userId, final int page, final int size) {
        return profileRepository.findLoyaltyHistoryByUserId(
                userId,
                PageRequest.of(page, size)
        );
    }

    public Page<ProfileUsedPromoProjection> getUsedPromosHistory(final UUID userId, final int page, final int size) {
        return profileRepository.findUsedPromosByUserId(
                userId,
                PageRequest.of(page, size)
        );
    }
}
