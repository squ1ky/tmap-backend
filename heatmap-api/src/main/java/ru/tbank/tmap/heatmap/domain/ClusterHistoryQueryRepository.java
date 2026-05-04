package ru.tbank.tmap.heatmap.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.heatmap.cluster.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.HeatmapClusterAggregate;

public interface ClusterHistoryQueryRepository {

    List<HeatmapClusterAggregate> findClusters(
            BoundingBox boundingBox,
            H3Resolution resolution,
            Instant from
    );

    Optional<ClusterDetailsAggregate> findClusterDetails(long h3Index, H3Resolution resolution, Instant from);
}
