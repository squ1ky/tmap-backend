package ru.tbank.tmap.venue.domain;

import java.util.UUID;

public class VenueNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final UUID venueId;

    public VenueNotFoundException(final UUID venueId) {
        super("Venue not found");
        this.venueId = venueId;
    }

    public UUID getVenueId() {
        return venueId;
    }
}
