package ru.tbank.tmap.venue.domain;

import ru.tbank.tmap.shared.geo.GeoPoint;

public record VenueCreateCommand(
        String name,
        String address,
        GeoPoint location,
        VenueCategory category,
        String description,
        String photoUrl,
        String dishOfDay,
        String music
) {
}
