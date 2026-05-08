package ru.tbank.tmap.venue.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
import ru.tbank.tmap.venue.domain.Venue;

@Component
@RequiredArgsConstructor
public class VenueH3Resolver {

    private final H3IndexService h3IndexService;

    public long toH3Res9(final GeoPoint location) {
        return h3IndexService.toH3(
                location.getLat(),
                location.getLng(),
                H3Resolution.RES_9
        );
    }

    public long resolveUpdatedH3Res9(final Venue venue, final GeoPoint newLocation) {
        if (venue.getContent().location().equals(newLocation)) {
            return venue.getContent().h3Res9();
        }
        return toH3Res9(newLocation);
    }
}
