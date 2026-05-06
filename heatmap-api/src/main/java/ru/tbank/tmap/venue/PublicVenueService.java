package ru.tbank.tmap.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.application.query.VenuePublicProjection;
import ru.tbank.tmap.venue.domain.repository.VenueQueryRepository;

@Service
@Transactional(readOnly = true)
public class PublicVenueService {

    private final VenueQueryRepository venueQueryRepository;

    public PublicVenueService(final VenueQueryRepository venueQueryRepository) {
        this.venueQueryRepository = venueQueryRepository;
    }

    public List<VenuePublicProjection> getVenuesInViewport(
            final BoundingBox boundingBox,
            final List<VenueCategory> categories
    ) {
        return venueQueryRepository.findActiveInViewport(boundingBox, categories);
    }

    public Optional<VenuePublicProjection> getVenueById(final UUID id) {
        return venueQueryRepository.findActiveById(id);
    }
}
