package ru.tbank.tmap.venue.search;

import java.util.List;

public interface VenueSearchService {

    List<VenueSearchResult> searchByName(String query);
}
