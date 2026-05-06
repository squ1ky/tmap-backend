package ru.tbank.tmap.venue.presentation.business;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.VenueModerationStatus;
import org.openapitools.model.VenueOwnerResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.presentation.VenueMapper;
import ru.tbank.tmap.venue.domain.Venue;

@Component
@RequiredArgsConstructor
public class BusinessVenueOwnerMapper {

    private final VenueMapper venuePublicMapper;

    public VenueOwnerResponse toResponse(final Venue venue) {
        return new VenueOwnerResponse()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .lat(venue.getLocation().getLat())
                .lng(venue.getLocation().getLng())
                .description(venue.getDescription())
                .category(VenueOwnerResponse.CategoryEnum.fromValue(
                        venue.getCategory().name().toLowerCase(Locale.ROOT)))
                .photoUrl(venuePublicMapper.toPublicPhotoUri(venue.getPhotoObjectKey()))
                .dishOfDay(venue.getDishOfDay())
                .music(venue.getMusic())
                .peopleNow(0)
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .promotions(List.of())
                .ownerId(venue.getOwner().getId())
                .h3Res9(Long.toUnsignedString(venue.getH3Res9()))
                .moderationStatus(VenueModerationStatus.fromValue(venue.getStatus().name()))
                .rejectReason(venue.getRejectReason());
    }
}
