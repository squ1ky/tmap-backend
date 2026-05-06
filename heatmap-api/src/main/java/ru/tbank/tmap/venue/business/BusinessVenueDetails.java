package ru.tbank.tmap.venue.business;

import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;

public record BusinessVenueDetails(
        Venue venue,
        VenuePendingUpdate pendingUpdate
) {
}
