package ru.tbank.tmap.venue.application.query;

import java.util.UUID;

import ru.tbank.tmap.venue.api.VenueCategory;

public record VenueSearchProjection(
        UUID id,
        String name,
        String address,
        double lat,
        double lng,
        VenueCategory category,
        String photoObjectKey
) {
}
