package ru.tbank.tmap.service;

import java.util.List;

import org.openapitools.model.VenueSearchResultResponse;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.mapper.VenueSearchMapper;
import ru.tbank.tmap.repository.VenueSearchRepository;

@Service
public class PublicVenueSearchService implements VenueSearchService {

    private final VenueSearchRepository venueSearchRepository;
    private final VenueSearchMapper venueSearchMapper;

    public PublicVenueSearchService(
            final VenueSearchRepository venueSearchRepository,
            final VenueSearchMapper venueSearchMapper
    ) {
        this.venueSearchRepository = venueSearchRepository;
        this.venueSearchMapper = venueSearchMapper;
    }

    @Override
    public List<VenueSearchResultResponse> searchByName(final String query) {
        final String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        return venueSearchRepository.searchByName(normalizedQuery)
                .stream()
                .map(venueSearchMapper::toResponse)
                .toList();
    }
}
