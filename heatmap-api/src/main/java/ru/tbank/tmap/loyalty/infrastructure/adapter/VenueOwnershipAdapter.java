package ru.tbank.tmap.loyalty.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.loyalty.application.exception.VenueAccessDeniedException;
import ru.tbank.tmap.loyalty.application.port.VenueOwnershipPort;
import ru.tbank.tmap.venue.api.VenueAccessFacade;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenueOwnershipAdapter implements VenueOwnershipPort {

    private final VenueAccessFacade venueAccessFacade;

    @Override
    public void requireOwner(UUID venueId, UUID ownerId) {
        final boolean isOwner = venueAccessFacade.isOwner(venueId, ownerId);

        if (!isOwner) {
            throw new VenueAccessDeniedException(venueId);
        }
    }
}
