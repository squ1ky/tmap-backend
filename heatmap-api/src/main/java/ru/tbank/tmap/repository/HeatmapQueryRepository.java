package ru.tbank.tmap.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ru.tbank.tmap.repository.model.ClusterDetailsAggregate;
import ru.tbank.tmap.repository.model.HeatmapClusterAggregate;

public interface HeatmapQueryRepository {

    List<HeatmapClusterAggregate> findClusters(
            double swLat,
            double swLng,
            double neLat,
            double neLng,
            int resolution,
            List<String> category,
            Instant from
    );

    Optional<ClusterDetailsAggregate> findClusterDetails(long h3Index, int resolution, Instant from);
}
