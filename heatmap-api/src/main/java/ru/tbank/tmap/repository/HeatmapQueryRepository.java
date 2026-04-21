package ru.tbank.tmap.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.geo.H3Resolution;
import ru.tbank.tmap.domain.venue.VenueCategory;
import ru.tbank.tmap.repository.model.ClusterDetailsAggregate;
import ru.tbank.tmap.repository.model.HeatmapClusterAggregate;

public interface HeatmapQueryRepository {

    List<HeatmapClusterAggregate> findClusters(
            BoundingBox boundingBox,
            H3Resolution resolution,
            List<VenueCategory> category,
            Instant from
    );

    Optional<ClusterDetailsAggregate> findClusterDetails(long h3Index, H3Resolution resolution, Instant from);
}
