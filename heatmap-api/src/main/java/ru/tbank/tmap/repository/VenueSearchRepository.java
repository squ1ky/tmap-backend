package ru.tbank.tmap.repository;

import java.util.List;

import ru.tbank.tmap.repository.model.VenueSearchResult;

public interface VenueSearchRepository {

    List<VenueSearchResult> searchByName(String query);
}
