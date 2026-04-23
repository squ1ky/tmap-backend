package ru.tbank.tmap.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openapitools.model.VenuePublicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.venue.VenueCategory;
import ru.tbank.tmap.mapper.VenuePublicMapper;
import ru.tbank.tmap.repository.VenueQueryRepository;

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
