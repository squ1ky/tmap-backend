package ru.tbank.tmap.heatmap.api.dto;

import java.time.Instant;
import java.util.List;

public record HeatmapResponse(
        int windowMinutes,
        Instant generatedAt,
        List<HeatmapClusterResponse> clusters
) {
}
