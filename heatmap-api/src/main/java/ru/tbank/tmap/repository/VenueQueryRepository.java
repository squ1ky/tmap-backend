package ru.tbank.tmap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.venue.VenueCategory;
import ru.tbank.tmap.repository.model.VenuePublicRow;

public interface VenueQueryRepository {

    List<VenuePublicRow> findActiveInViewport(BoundingBox boundingBox, List<VenueCategory> categories);

    Optional<VenuePublicRow> findActiveById(UUID id);
}
