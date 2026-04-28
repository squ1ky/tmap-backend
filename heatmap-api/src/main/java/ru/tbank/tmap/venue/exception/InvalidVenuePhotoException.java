package ru.tbank.tmap.venue.exception;

public class InvalidVenuePhotoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidVenuePhotoException(String message) {
        super(message);
    }
}
