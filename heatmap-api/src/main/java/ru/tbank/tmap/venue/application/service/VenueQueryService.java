package ru.tbank.tmap.venue.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.application.query.VenueProjection;
import ru.tbank.tmap.venue.api.VenueCategory;
import ru.tbank.tmap.venue.domain.repository.VenueQueryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueQueryService {

    private final VenueQueryRepository venueQueryRepository;

    public List<VenueProjection> getVenuesInViewport(
            final BoundingBox boundingBox,
            final List<VenueCategory> categories
    ) {
        return venueQueryRepository.findActiveInViewport(boundingBox, categories);
    }

    public Optional<VenueProjection> getVenueById(final UUID id) {
        return venueQueryRepository.findActiveById(id);
    }
}
