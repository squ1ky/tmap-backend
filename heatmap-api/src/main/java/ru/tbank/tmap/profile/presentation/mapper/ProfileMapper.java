package ru.tbank.tmap.profile.presentation.mapper;

import org.openapitools.model.LoyaltyQrResponse;
import org.openapitools.model.LoyaltyVerificationPage;
import org.openapitools.model.LoyaltyVerificationResponse;
import org.openapitools.model.ProfileResponse;
import org.openapitools.model.UsedPromoPage;
import org.openapitools.model.UsedPromoResponse;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.application.query.UsedPromoProjection;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.profile.application.query.ProfileLoyaltyQr;
import ru.tbank.tmap.user.api.UserView;

@Component
public class ProfileMapper {

    public ProfileResponse toResponse(final UserView profile) {
        return new ProfileResponse()
                .id(profile.id())
                .email(profile.email())
                .nickname(profile.nickname())
                .role(UserRole.fromValue(profile.role().name()));
    }

    public LoyaltyQrResponse toResponse(final ProfileLoyaltyQr qrView) {
        return new LoyaltyQrResponse()
                .userId(qrView.userId())
                .qrPayload(qrView.qrPayload());
    }

    public LoyaltyVerificationPage toHistoryPageResponse(final Page<LoyaltyHistoryProjection> pageView) {
        return new LoyaltyVerificationPage()
                .items(pageView.getContent().stream().map(this::toResponse).toList())
                .page(pageView.getNumber())
                .size(pageView.getSize())
                .totalPages(pageView.getTotalPages())
                .totalElements(pageView.getTotalElements());
    }

    public UsedPromoPage toUsedPromoPageResponse(final Page<UsedPromoProjection> pageView) {
        return new UsedPromoPage()
                .items(pageView.getContent().stream().map(this::toResponse).toList())
                .page(pageView.getNumber())
                .size(pageView.getSize())
                .totalPages(pageView.getTotalPages())
                .totalElements(pageView.getTotalElements());
    }

    private LoyaltyVerificationResponse toResponse(final LoyaltyHistoryProjection item) {
        return new LoyaltyVerificationResponse()
                .id(item.id())
                .venueId(item.venueId())
                .venueName(item.venueName())
                .ruleId(item.ruleId())
                .ruleDescription(item.ruleDescription())
                .discountApplied(item.discountApplied())
                .verifiedAt(item.verifiedAt());
    }

    private UsedPromoResponse toResponse(final UsedPromoProjection item) {
        return new UsedPromoResponse()
                .venueName(item.venueName())
                .description(item.description())
                .discountPercent(item.discountPercent())
                .usedAt(item.usedAt());
    }
}
