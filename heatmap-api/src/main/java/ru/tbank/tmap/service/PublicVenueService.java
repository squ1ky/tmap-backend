package ru.tbank.tmap.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.openapitools.model.VenuePublicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.venue.VenueCategory;
import ru.tbank.tmap.repository.VenueQueryRepository;
import ru.tbank.tmap.repository.model.VenuePublicRow;

@Service
@Transactional(readOnly = true)
public class PublicVenueService implements VenueService {

    private final VenueQueryRepository venueQueryRepository;

    public PublicVenueService(final VenueQueryRepository venueQueryRepository) {
        this.venueQueryRepository = venueQueryRepository;
    }

    @Override
    public List<VenuePublicResponse> getVenuesInViewport(
            final BoundingBox boundingBox,
            final List<VenueCategory> categories
    ) {
        return venueQueryRepository.findActiveInViewport(boundingBox, categories).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<VenuePublicResponse> getVenueById(final UUID id) {
        return venueQueryRepository.findActiveById(id)
                .map(this::toResponse);
    }

    private VenuePublicResponse toResponse(final VenuePublicRow venue) {
        return new VenuePublicResponse()
                .id(venue.id())
                .name(venue.name())
                .address(venue.address())
                .lat(venue.lat())
                .lng(venue.lng())
                .description(venue.description())
                .category(VenuePublicResponse.CategoryEnum.fromValue(
                        venue.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(toUri(venue.photoUrl()))
                .dishOfDay(venue.dishOfDay())
                .music(venue.music())
                .peopleNow(0)
                .createdAt(venue.createdAt())
                .updatedAt(venue.updatedAt())
                .promotions(List.of());
    }

    private URI toUri(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URI.create(value);
    }
}
