package ru.tbank.tmap.venue.presentation.business;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.LoyaltyRuleResponse;
import org.openapitools.model.VenueModerationStatus;
import org.openapitools.model.VenueOwnerResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.domain.VenueContent;
import ru.tbank.tmap.venue.presentation.VenueMapper;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;

@Component
@RequiredArgsConstructor
public class BusinessVenueOwnerMapper {

    private final VenueMapper venueMapper;

    public VenueOwnerResponse toResponse(final Venue venue) {
        return toResponse(venue, List.of());
    }

    public VenueOwnerResponse toResponse(final Venue venue, final List<LoyaltyRuleResponse> promotions) {
        return toResponse(new VenueDetails(venue, null), promotions);
    }

    public VenueOwnerResponse toResponse(final VenueDetails details) {
        return toResponse(details, List.of());
    }

    public VenueOwnerResponse toResponse(final VenueDetails details, final List<LoyaltyRuleResponse> promotions) {
        final Venue venue = details.venue();
        final VenuePendingUpdate pendingUpdate = details.pendingUpdate();
        final VenueContent content = details.displayContent();
        final VenueStatus moderationStatus = details.displayStatus();
        final String rejectReason = details.displayRejectReason();

        return new VenueOwnerResponse()
                .id(venue.getId())
                .ownerId(venue.getOwnerId())
                .name(content.name())
                .address(content.address())
                .lat(content.location().getLat())
                .lng(content.location().getLng())
                .description(content.description())
                .category(VenueOwnerResponse.CategoryEnum.fromValue(
                        content.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(venueMapper.toPublicPhotoUri(venue.getPhotoObjectKey()))
                .dishOfDay(content.dishOfDay())
                .music(content.music())
                .peopleNow(0)
                .createdAt(venue.getCreatedAt())
                .updatedAt(pendingUpdate != null ? pendingUpdate.getUpdatedAt() : venue.getUpdatedAt())
                .promotions(promotions)
                .h3Res9(Long.toUnsignedString(content.h3Res9()))
                .moderationStatus(VenueModerationStatus.fromValue(moderationStatus.name()))
                .rejectReason(rejectReason);
    }
}
