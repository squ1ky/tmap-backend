package ru.tbank.tmap.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openapitools.model.VenuePublicResponse;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.domain.VenueCategory;

public interface VenueService {

    List<VenuePublicResponse> getVenuesInViewport(BoundingBox boundingBox, List<VenueCategory> categories);

    Optional<VenuePublicResponse> getVenueById(UUID id);
}
