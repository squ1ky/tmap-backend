package ru.tbank.tmap.profile.presentation.mapper;

import org.openapitools.model.LoyaltyVerificationPage;
import org.openapitools.model.LoyaltyVerificationResponse;
import org.openapitools.model.ProfileResponse;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import java.util.Locale;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import org.springframework.stereotype.Component;
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

    public LoyaltyVerificationPage toHistoryPageResponse(final Page<LoyaltyHistoryProjection> pageView) {
        return new LoyaltyVerificationPage()
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
                .category(LoyaltyVerificationResponse.CategoryEnum.fromValue(
                        item.venueCategory().toLowerCase(Locale.ROOT)
                ))
                .ruleId(item.ruleId())
                .ruleDescription(item.ruleDescription())
                .discountApplied(item.discountApplied())
                .verifiedAt(item.verifiedAt());
    }
}
