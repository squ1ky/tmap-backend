package ru.tbank.tmap.loyalty.application.port;

import java.util.UUID;

public interface VenueOwnershipPort {

    /**
     * @throws ru.tbank.tmap.loyalty.application.exception.VenueAccessDeniedException
     *         if the venue does not exist or is not owned by the given user
     */
    void requireOwner(UUID venueId, UUID ownerId);
}
