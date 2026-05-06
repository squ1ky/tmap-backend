package ru.tbank.tmap.venue.application.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.venue.application.query.VenueSearchProjection;
import ru.tbank.tmap.venue.domain.repository.VenueSearchRepository;

@Service
@RequiredArgsConstructor
public class VenueSearchService {

    private final VenueSearchRepository venueSearchRepository;

    public List<VenueSearchProjection> searchByName(final String query) {
        final String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        return venueSearchRepository.searchByName(normalizedQuery);
    }
}
