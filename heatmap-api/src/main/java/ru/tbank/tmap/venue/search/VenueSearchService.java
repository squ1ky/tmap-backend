package ru.tbank.tmap.venue.search;

import java.util.List;

import org.openapitools.model.VenueSearchResultResponse;

public interface VenueSearchService {

    List<VenueSearchResultResponse> searchByName(String query);
}
