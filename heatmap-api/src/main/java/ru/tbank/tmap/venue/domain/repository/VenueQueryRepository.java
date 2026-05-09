package ru.tbank.tmap.venue.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.application.query.VenueProjection;

public interface VenueQueryRepository {

    List<VenueProjection> findActiveInViewport(BoundingBox boundingBox, List<VenueCategory> categories);

    Optional<VenueProjection> findActiveById(UUID id);
}
