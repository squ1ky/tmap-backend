package ru.tbank.tmap.venue.exception;

import java.util.UUID;
import ru.tbank.tmap.venue.domain.VenueStatus;

public class VenueModerationStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final UUID venueId;
    private final VenueStatus status;

    public VenueModerationStateException(final UUID venueId, final VenueStatus status) {
        super("Only PENDING venues can be moderated");
        this.venueId = venueId;
        this.status = status;
    }

    public UUID getVenueId() {
        return venueId;
    }

    public VenueStatus getStatus() {
        return status;
    }
}
