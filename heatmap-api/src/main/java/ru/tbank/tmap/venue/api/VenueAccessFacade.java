package ru.tbank.tmap.venue.api;

import java.util.UUID;

public interface VenueAccessFacade {

    boolean isOwner(UUID venueId, UUID ownerId);
}
