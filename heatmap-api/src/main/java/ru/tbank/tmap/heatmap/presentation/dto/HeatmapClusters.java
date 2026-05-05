package ru.tbank.tmap.heatmap.presentation.dto;

import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;

import java.time.OffsetDateTime;
import java.util.List;

public record HeatmapClusters(
        OffsetDateTime generatedAt,
        int refreshIntervalMinutes,
        int aggregationWindowMinutes,
        List<HeatmapClusterAggregate> clusters
) {
}
