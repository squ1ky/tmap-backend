package ru.tbank.tmap.loyalty.application.exception;

import java.util.UUID;

public class VenueAccessDeniedException extends RuntimeException {
    public VenueAccessDeniedException(UUID venueId) {
        super("Venue not accessible " + venueId);
    }
}
