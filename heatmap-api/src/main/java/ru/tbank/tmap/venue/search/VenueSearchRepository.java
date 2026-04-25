package ru.tbank.tmap.venue.search;

import java.util.List;

public interface VenueSearchRepository {

    List<VenueSearchResult> searchByName(String query);
}
