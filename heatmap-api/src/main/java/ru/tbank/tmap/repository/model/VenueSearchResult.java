package ru.tbank.tmap.repository.model;

import java.util.UUID;

import ru.tbank.tmap.domain.venue.VenueCategory;

public record VenueSearchResult(
        UUID id,
        String name,
        String address,
        double lat,
        double lng,
        VenueCategory category,
        String photoUrl
) {
}
