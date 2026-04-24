package ru.tbank.tmap.service;

import java.util.List;

import org.openapitools.model.VenueSearchResultResponse;

public interface VenueSearchService {

    List<VenueSearchResultResponse> searchByName(String query);
}
