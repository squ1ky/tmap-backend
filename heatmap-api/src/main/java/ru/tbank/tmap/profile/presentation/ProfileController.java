package ru.tbank.tmap.profile.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ProfileApi;
import org.openapitools.model.ChangePasswordRequest;
import org.openapitools.model.LoyaltyQrResponse;
import org.openapitools.model.LoyaltyVerificationPage;
import org.openapitools.model.ProfileResponse;
import org.openapitools.model.UsedPromoPage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.profile.application.ProfileService;
import ru.tbank.tmap.profile.presentation.mapper.ProfileMapper;
import ru.tbank.tmap.shared.utils.SecurityUtils;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;
    private final ProfileMapper profileMapper;

    @Override
    public ResponseEntity<ProfileResponse> getMyProfile() {
        return ResponseEntity.ok(profileMapper.toResponse(
                profileService.getProfile(SecurityUtils.currentUserId())
        ));
    }

    @Override
    public ResponseEntity<Void> changeMyPassword(@Valid final ChangePasswordRequest changePasswordRequest) {
        profileService.changePassword(SecurityUtils.currentUserId(), changePasswordRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LoyaltyQrResponse> getMyLoyaltyQr() {
        return ResponseEntity.ok(profileMapper.toResponse(
                profileService.getLoyaltyQr(SecurityUtils.currentUserId())
        ));
    }

    @Override
    public ResponseEntity<LoyaltyVerificationPage> getLoyaltyHistory(final Integer page, final Integer size) {
        return ResponseEntity.ok(profileMapper.toHistoryPageResponse(
                profileService.getLoyaltyHistory(SecurityUtils.currentUserId(), page, size)
        ));
    }

    @Override
    public ResponseEntity<UsedPromoPage> getUsedPromosHistory(final Integer page, final Integer size) {
        return ResponseEntity.ok(profileMapper.toUsedPromoPageResponse(
                profileService.getUsedPromosHistory(SecurityUtils.currentUserId(), page, size)
        ));
    }
}
