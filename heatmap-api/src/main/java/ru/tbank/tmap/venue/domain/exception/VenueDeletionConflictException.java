package ru.tbank.tmap.venue.domain.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public class VenueDeletionConflictException extends RuntimeException {
    private static final String HISTORY_MESSAGE =
            "Venue cannot be deleted because it has related history records";

    private final UUID venueId;

    public VenueDeletionConflictException(final UUID venueId, final String message) {
        super(message);
        this.venueId = venueId;
    }

    public VenueDeletionConflictException(final UUID venueId, final String message, final Throwable cause) {
        super(message, cause);
        this.venueId = venueId;
    }

    public static VenueDeletionConflictException forRelatedHistory(final UUID venueId, final Throwable cause) {
        return new VenueDeletionConflictException(venueId, HISTORY_MESSAGE, cause);
    }
}
