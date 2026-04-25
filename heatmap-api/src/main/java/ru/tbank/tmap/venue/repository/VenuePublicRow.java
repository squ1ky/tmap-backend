package ru.tbank.tmap.venue.repository;

import java.time.OffsetDateTime;
import java.util.UUID;
import ru.tbank.tmap.venue.domain.VenueCategory;

public record VenuePublicRow(
        UUID id,
        String name,
        String address,
        double lat,
        double lng,
        String description,
        VenueCategory category,
        String photoUrl,
        String dishOfDay,
        String music,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
