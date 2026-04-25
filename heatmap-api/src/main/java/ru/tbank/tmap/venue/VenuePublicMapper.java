package ru.tbank.tmap.venue;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenuePublicResponse;
import org.openapitools.model.VenueSearchResultResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.repository.VenuePublicRow;
import ru.tbank.tmap.venue.search.VenueSearchResult;

@Component
public class VenuePublicMapper {

    public VenuePublicResponse toResponse(final VenuePublicRow venue) {
        return new VenuePublicResponse()
                .id(venue.id())
                .name(venue.name())
                .address(venue.address())
                .lat(venue.lat())
                .lng(venue.lng())
                .description(venue.description())
                .category(VenuePublicResponse.CategoryEnum.fromValue(
                        venue.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(toUri(venue.photoUrl()))
                .dishOfDay(venue.dishOfDay())
                .music(venue.music())
                .peopleNow(0)
                .createdAt(venue.createdAt())
                .updatedAt(venue.updatedAt())
                .promotions(List.of());
    }

    public VenueSearchResultResponse toSearchResponse(final VenueSearchResult venue) {
        return new VenueSearchResultResponse()
                .id(venue.id())
                .name(venue.name())
                .address(venue.address())
                .lat(venue.lat())
                .lng(venue.lng())
                .category(VenueSearchResultResponse.CategoryEnum.fromValue(
                        venue.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(toUri(venue.photoUrl()));
    }

    public AdminVenueModerationResponse toAdminModerationResponse(final Venue venue) {
        return new AdminVenueModerationResponse()
                .id(venue.getId())
                .ownerId(venue.getOwner().getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .lat(venue.getLocation().getLat())
                .lng(venue.getLocation().getLng())
                .h3Res9(Long.toUnsignedString(venue.getH3Res9()))
                .category(AdminVenueModerationResponse.CategoryEnum.fromValue(
                        venue.getCategory().name().toLowerCase(Locale.ROOT)))
                .moderationStatus(VenueModerationStatus.fromValue(venue.getStatus().name()))
                .rejectReason(venue.getRejectReason())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt());
    }

    private URI toUri(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URI.create(value);
    }
}
