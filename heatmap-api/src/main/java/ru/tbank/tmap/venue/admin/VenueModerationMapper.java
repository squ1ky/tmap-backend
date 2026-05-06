package ru.tbank.tmap.venue.admin;

import java.util.Locale;
import java.time.OffsetDateTime;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;

@Component
public class VenueModerationMapper {

    public AdminVenueModerationPage toPage(final Page<VenueModerationDetails> venues) {
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
        return toResponse(new VenueModerationDetails(venue, null));
    }

    public AdminVenueModerationResponse toResponse(final VenueModerationDetails details) {
        final VenueModerationSnapshot snapshot = toSnapshot(details);
        return new AdminVenueModerationResponse()
                .id(snapshot.id())
                .ownerId(snapshot.ownerId())
                .name(snapshot.name())
                .address(snapshot.address())
                .lat(snapshot.lat())
                .lng(snapshot.lng())
                .h3Res9(Long.toUnsignedString(snapshot.h3Res9()))
                .category(AdminVenueModerationResponse.CategoryEnum.fromValue(
                        snapshot.category().name().toLowerCase(Locale.ROOT)))
                .moderationStatus(VenueModerationStatus.fromValue(snapshot.moderationStatus().name()))
                .rejectReason(snapshot.rejectReason())
                .createdAt(snapshot.createdAt())
                .updatedAt(snapshot.updatedAt());
    }

    private VenueModerationSnapshot toSnapshot(final VenueModerationDetails details) {
        final VenuePendingUpdate pendingUpdate = details.pendingUpdate();
        if (pendingUpdate == null) {
            return VenueModerationSnapshot.fromVenue(details.venue());
        }
        return VenueModerationSnapshot.fromPendingUpdate(details.venue(), pendingUpdate);
    }

    private record VenueModerationSnapshot(
            java.util.UUID id,
            java.util.UUID ownerId,
            String name,
            String address,
            double lat,
            double lng,
            long h3Res9,
            VenueCategory category,
            ru.tbank.tmap.venue.domain.VenueStatus moderationStatus,
            String rejectReason,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        private static VenueModerationSnapshot fromVenue(final Venue venue) {
            return new VenueModerationSnapshot(
                    venue.getId(),
                    venue.getOwner().getId(),
                    venue.getName(),
                    venue.getAddress(),
                    venue.getLocation().getLat(),
                    venue.getLocation().getLng(),
                    venue.getH3Res9(),
                    venue.getCategory(),
                    venue.getStatus(),
                    venue.getRejectReason(),
                    venue.getCreatedAt(),
                    venue.getUpdatedAt()
            );
        }

        private static VenueModerationSnapshot fromPendingUpdate(
                final Venue venue,
                final VenuePendingUpdate pendingUpdate
        ) {
            return new VenueModerationSnapshot(
                    venue.getId(),
                    venue.getOwner().getId(),
                    pendingUpdate.getName(),
                    pendingUpdate.getAddress(),
                    pendingUpdate.getLocation().getLat(),
                    pendingUpdate.getLocation().getLng(),
                    pendingUpdate.getH3Res9(),
                    pendingUpdate.getCategory(),
                    pendingUpdate.getStatus(),
                    pendingUpdate.getRejectReason(),
                    venue.getCreatedAt(),
                    pendingUpdate.getUpdatedAt()
            );
        }
    }
}
