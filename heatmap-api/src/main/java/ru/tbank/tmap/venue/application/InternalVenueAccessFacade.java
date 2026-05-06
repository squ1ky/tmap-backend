package ru.tbank.tmap.venue.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.venue.api.VenueAccessFacade;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalVenueAccessFacade implements VenueAccessFacade {

    private final VenueRepository venueRepository;

    @Override
    public boolean isOwner(UUID venueId, UUID ownerId) {
        return venueRepository.existsByIdAndOwnerId(venueId, ownerId);
    }
}
