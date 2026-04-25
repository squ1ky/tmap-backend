package ru.tbank.tmap.venue.search;

import java.util.UUID;

import ru.tbank.tmap.venue.domain.VenueCategory;

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
