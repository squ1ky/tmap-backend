package ru.tbank.tmap.venue.admin;

import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;

public record VenueModerationDetails(
        Venue venue,
        VenuePendingUpdate pendingUpdate
) {
}
