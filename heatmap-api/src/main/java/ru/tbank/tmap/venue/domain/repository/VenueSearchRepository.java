package ru.tbank.tmap.venue.domain.repository;

import ru.tbank.tmap.venue.application.query.VenueSearchProjection;

import java.util.List;

public interface VenueSearchRepository {

    List<VenueSearchProjection> searchByName(String query);
}
