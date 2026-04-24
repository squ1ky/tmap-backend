package ru.tbank.tmap.service;

import java.util.List;

import org.openapitools.model.VenueSearchResultResponse;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.mapper.VenuePublicMapper;
import ru.tbank.tmap.repository.VenueSearchRepository;

@Service
public class PublicVenueSearchService implements VenueSearchService {

    private final VenueSearchRepository venueSearchRepository;
    private final VenuePublicMapper venuePublicMapper;

    public PublicVenueSearchService(
            final VenueSearchRepository venueSearchRepository,
            final VenuePublicMapper venuePublicMapper
    ) {
        this.venueSearchRepository = venueSearchRepository;
        this.venuePublicMapper = venuePublicMapper;
    }

    @Override
    public List<VenueSearchResultResponse> searchByName(final String query) {
        final String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        return venueSearchRepository.searchByName(normalizedQuery)
                .stream()
                .map(venuePublicMapper::toSearchResponse)
                .toList();
    }
}
