package ru.tbank.tmap.venue.presentation.admin;

import java.util.Locale;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.domain.Venue;

@Component
public class AdminVenueMapper {

    private static final int MAX_TOTAL_ELEMENTS = Integer.MAX_VALUE;

    public AdminVenueModerationPage toPage(final Page<Venue> venues) {
        return new AdminVenueModerationPage()
                .items(venues.stream()
                        .map(this::toResponse)
                        .toList())
                .page(venues.getNumber())
                .size(venues.getSize())
                .totalPages(venues.getTotalPages())
                .totalElements(toIntTotalElements(venues.getTotalElements()));
    }

    public AdminVenueModerationResponse toResponse(final Venue venue) {
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

    private int toIntTotalElements(final long totalElements) {
        if (totalElements > MAX_TOTAL_ELEMENTS) {
            return MAX_TOTAL_ELEMENTS;
        }
        return (int) totalElements;
    }
}
