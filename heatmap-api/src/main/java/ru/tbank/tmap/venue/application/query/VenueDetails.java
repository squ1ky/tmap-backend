package ru.tbank.tmap.venue.application.query;

import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;

public record VenueDetails(
        Venue venue,
        VenuePendingUpdate pendingUpdate
) {
}
