package ru.tbank.tmap.heatmap.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.heatmap.application.query.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;

public interface ClusterHistoryQueryRepository {

    List<HeatmapClusterAggregate> findClustersByParents(
            List<Long> parentRes6Indexes,
            H3Resolution resolution,
            Instant from,
            Instant currentHour
    );

    Optional<ClusterDetailsAggregate> findClusterDetails(
            long h3Index,
            H3Resolution resolution,
            Instant from,
            Instant currentHour
    );
}
