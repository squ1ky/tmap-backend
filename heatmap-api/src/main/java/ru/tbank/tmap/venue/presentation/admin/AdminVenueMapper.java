package ru.tbank.tmap.venue.presentation.admin;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.application.query.VenueDetailsWithOwnerEmail;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueContent;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.presentation.VenueMapper;

@Component
@RequiredArgsConstructor
public class AdminVenueMapper {

    private final VenueMapper venueMapper;

    public AdminVenueModerationPage toPage(final Page<VenueDetailsWithOwnerEmail> venues) {
        return new AdminVenueModerationPage()
                .items(venues.stream()
                        .map(this::toResponse)
                        .toList())
                .page(venues.getNumber())
                .size(venues.getSize())
                .totalPages(venues.getTotalPages())
                .totalElements(venues.getTotalElements());
    }

    public AdminVenueModerationResponse toResponse(final Venue venue) {
        return toResponse(new VenueDetails(venue, null));
    }

    public AdminVenueModerationResponse toResponse(final VenueDetails details) {
        return buildResponse(details, null);
    }

    public AdminVenueModerationResponse toResponse(final VenueDetailsWithOwnerEmail withOwnerEmail) {
        return buildResponse(withOwnerEmail.venueDetails(), withOwnerEmail.ownerEmail());
    }

    private AdminVenueModerationResponse buildResponse(final VenueDetails details, final String ownerEmail) {
        final Venue venue = details.venue();
        final VenueContent content = details.displayContent();
        final String photoObjectKey = details.displayPhotoObjectKey();
        final VenueStatus moderationStatus = details.displayStatus();
        final String rejectReason = details.displayRejectReason();

        return new AdminVenueModerationResponse()
                .id(venue.getId())
                .ownerId(venue.getOwnerId())
                .ownerEmail(ownerEmail)
                .name(content.name())
                .address(content.address())
                .lat(content.location().getLat())
                .lng(content.location().getLng())
                .h3Res9(Long.toUnsignedString(content.h3Res9()))
                .category(AdminVenueModerationResponse.CategoryEnum.fromValue(
                        content.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(venueMapper.toPublicPhotoUri(photoObjectKey))
                .dishOfDay(content.dishOfDay())
                .music(content.music())
                .moderationStatus(VenueModerationStatus.fromValue(moderationStatus.name()))
                .rejectReason(rejectReason)
                .createdAt(venue.getCreatedAt())
                .updatedAt(details.displayUpdatedAt());
    }
}
