package ru.tbank.tmap.venue.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;
import ru.tbank.tmap.venue.domain.VenueCategory;

public record VenueProjection(
        UUID id,
        String name,
        String address,
        double lat,
        double lng,
        String description,
        VenueCategory category,
        String photoObjectKey,
        String dishOfDay,
        String music,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
