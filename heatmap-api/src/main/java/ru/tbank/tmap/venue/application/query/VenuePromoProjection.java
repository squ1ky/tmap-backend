package ru.tbank.tmap.venue.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VenuePromoProjection(
        UUID id,
        UUID venueId,
        String title,
        String description,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime createdAt
) {
}
