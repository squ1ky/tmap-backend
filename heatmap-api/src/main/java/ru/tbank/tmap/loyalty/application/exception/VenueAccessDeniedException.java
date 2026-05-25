package ru.tbank.tmap.loyalty.application.exception;

import java.util.UUID;

public class VenueAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_PREFIX = "Venue not accessible ";

    public VenueAccessDeniedException(final UUID venueId) {
        super(MESSAGE_PREFIX + venueId);
    }
}
