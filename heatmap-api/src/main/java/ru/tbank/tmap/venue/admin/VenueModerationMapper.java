package ru.tbank.tmap.venue.admin;

import java.util.Locale;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.domain.Venue;

@Component
public class VenueModerationMapper {

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
}
