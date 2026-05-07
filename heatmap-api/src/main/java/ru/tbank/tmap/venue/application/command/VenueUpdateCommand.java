package ru.tbank.tmap.venue.application.command;

import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.venue.domain.VenueCategory;

public record VenueUpdateCommand(
        String name,
        String address,
        GeoPoint location,
        VenueCategory category,
        String description,
        String dishOfDay,
        String music
) {
}
