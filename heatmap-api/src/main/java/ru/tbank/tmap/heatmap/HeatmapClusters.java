package ru.tbank.tmap.heatmap;

import java.time.OffsetDateTime;
import java.util.List;

public record HeatmapClusters(
        OffsetDateTime generatedAt,
        int refreshIntervalMinutes,
        int aggregationWindowMinutes,
        List<HeatmapClusterAggregate> clusters
) {
}
