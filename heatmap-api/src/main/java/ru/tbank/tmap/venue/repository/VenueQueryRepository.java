package ru.tbank.tmap.venue.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.domain.VenueCategory;

public interface VenueQueryRepository {

    List<VenuePublicRow> findActiveInViewport(BoundingBox boundingBox, List<VenueCategory> categories);

    Optional<VenuePublicRow> findActiveById(UUID id);
}
