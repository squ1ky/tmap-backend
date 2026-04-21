package ru.tbank.tmap.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import ru.tbank.tmap.domain.geo.H3Resolution;
import ru.tbank.tmap.repository.model.ClusterDetailsAggregate;
import ru.tbank.tmap.repository.model.HeatmapClusterAggregate;

public interface HeatmapQueryRepository {

    List<HeatmapClusterAggregate> findClusters(
            double swLat,
            double swLng,
            double neLat,
            double neLng,
            H3Resolution resolution,
            List<String> category,
            Instant from
    );

    Optional<ClusterDetailsAggregate> findClusterDetails(long h3Index, H3Resolution resolution, Instant from);
}
