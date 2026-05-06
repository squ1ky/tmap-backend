package ru.tbank.tmap.venue.search;

import java.util.List;

import org.springframework.stereotype.Service;
import ru.tbank.tmap.venue.application.query.VenueSearchProjection;
import ru.tbank.tmap.venue.domain.repository.VenueSearchRepository;

@Service
public class PublicVenueSearchService implements VenueSearchService {

    private final VenueSearchRepository venueSearchRepository;

    public PublicVenueSearchService(final VenueSearchRepository venueSearchRepository) {
        this.venueSearchRepository = venueSearchRepository;
    }

    @Override
    public List<VenueSearchProjection> searchByName(final String query) {
        final String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        return venueSearchRepository.searchByName(normalizedQuery);
    }
}
