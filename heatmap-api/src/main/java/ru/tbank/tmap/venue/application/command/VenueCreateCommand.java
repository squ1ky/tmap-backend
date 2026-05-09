package ru.tbank.tmap.venue.application.command;

import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueContent;

public record VenueCreateCommand(
        String name,
        String address,
        GeoPoint location,
        VenueCategory category,
        String description,
        String dishOfDay,
        String music
) {
    public VenueContent toContent(final long h3Res9) {
        return new VenueContent(
                name,
                address,
                location,
                h3Res9,
                category,
                description,
                dishOfDay,
                music
        );
    }
}
