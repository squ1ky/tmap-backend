package ru.tbank.tmap.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openapitools.model.VenuePublicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.repository.VenueQueryRepository;

@Service
@Transactional(readOnly = true)
public class PublicVenueService implements VenueService {

    private final VenueQueryRepository venueQueryRepository;
    private final VenuePublicMapper venuePublicMapper;

    public PublicVenueService(
            final VenueQueryRepository venueQueryRepository,
            final VenuePublicMapper venuePublicMapper
    ) {
        this.venueQueryRepository = venueQueryRepository;
        this.venuePublicMapper = venuePublicMapper;
    }

    @Override
    public List<VenuePublicResponse> getVenuesInViewport(
            final BoundingBox boundingBox,
            final List<VenueCategory> categories
    ) {
        return venueQueryRepository.findActiveInViewport(boundingBox, categories).stream()
                .map(venuePublicMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<VenuePublicResponse> getVenueById(final UUID id) {
        return venueQueryRepository.findActiveById(id)
                .map(venuePublicMapper::toResponse);
    }
}
