package ru.tbank.tmap.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openapitools.model.VenuePublicResponse;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.venue.VenueCategory;

public interface VenueService {

    List<VenuePublicResponse> getVenuesInViewport(BoundingBox boundingBox, List<VenueCategory> categories);

    Optional<VenuePublicResponse> getVenueById(UUID id);
}
