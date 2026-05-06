package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.Locale;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.VenueModerationStatus;
import org.openapitools.model.VenueOwnerResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.venue.application.VenueDetails;
import ru.tbank.tmap.venue.VenuePublicMapper;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;

@Component
@RequiredArgsConstructor
public class VenueOwnerMapper {

    private final VenuePublicMapper venuePublicMapper;

    public VenueOwnerResponse toResponse(final Venue venue) {
        return toResponse(new VenueDetails(venue, null));
    }

    public VenueOwnerResponse toResponse(final VenueDetails details) {
        final VenueOwnerSnapshot snapshot = toSnapshot(details);
        return new VenueOwnerResponse()
                .id(snapshot.id())
                .name(snapshot.name())
                .address(snapshot.address())
                .lat(snapshot.lat())
                .lng(snapshot.lng())
                .description(snapshot.description())
                .category(VenueOwnerResponse.CategoryEnum.fromValue(
                        snapshot.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(venuePublicMapper.toPublicPhotoUri(snapshot.photoObjectKey()))
                .dishOfDay(snapshot.dishOfDay())
                .music(snapshot.music())
                .peopleNow(0)
                .createdAt(snapshot.createdAt())
                .updatedAt(snapshot.updatedAt())
                .promotions(List.of())
                .ownerId(snapshot.ownerId())
                .h3Res9(Long.toUnsignedString(snapshot.h3Res9()))
                .moderationStatus(VenueModerationStatus.fromValue(snapshot.moderationStatus().name()))
                .rejectReason(snapshot.rejectReason());
    }

    private VenueOwnerSnapshot toSnapshot(final VenueDetails details) {
        final VenuePendingUpdate pendingUpdate = details.pendingUpdate();
        if (pendingUpdate == null) {
            return VenueOwnerSnapshot.fromVenue(details.venue());
        }
        return VenueOwnerSnapshot.fromVenueWithPendingUpdate(details.venue(), pendingUpdate);
    }

    private record VenueOwnerSnapshot(
            java.util.UUID id,
            java.util.UUID ownerId,
            String name,
            String address,
            double lat,
            double lng,
            String description,
            VenueCategory category,
            String photoObjectKey,
            String dishOfDay,
            String music,
            long h3Res9,
            VenueStatus moderationStatus,
            String rejectReason,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        private static VenueOwnerSnapshot fromVenue(final Venue venue) {
            return new VenueOwnerSnapshot(
                    venue.getId(),
                    venue.getOwner().getId(),
                    venue.getName(),
                    venue.getAddress(),
                    venue.getLocation().getLat(),
                    venue.getLocation().getLng(),
                    venue.getDescription(),
                    venue.getCategory(),
                    venue.getPhotoObjectKey(),
                    venue.getDishOfDay(),
                    venue.getMusic(),
                    venue.getH3Res9(),
                    venue.getStatus(),
                    venue.getRejectReason(),
                    venue.getCreatedAt(),
                    venue.getUpdatedAt()
            );
        }

        private static VenueOwnerSnapshot fromVenueWithPendingUpdate(
                final Venue venue,
                final VenuePendingUpdate pendingUpdate
        ) {
            return new VenueOwnerSnapshot(
                    venue.getId(),
                    venue.getOwner().getId(),
                    venue.getName(),
                    venue.getAddress(),
                    venue.getLocation().getLat(),
                    venue.getLocation().getLng(),
                    venue.getDescription(),
                    venue.getCategory(),
                    venue.getPhotoObjectKey(),
                    venue.getDishOfDay(),
                    venue.getMusic(),
                    venue.getH3Res9(),
                    pendingUpdate.getStatus(),
                    pendingUpdate.getRejectReason(),
                    venue.getCreatedAt(),
                    pendingUpdate.getUpdatedAt()
            );
        }
    }
}
