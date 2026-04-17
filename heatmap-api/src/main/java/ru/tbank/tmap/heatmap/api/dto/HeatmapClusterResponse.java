package ru.tbank.tmap.heatmap.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HeatmapClusterResponse(
        double centerLat,
        double centerLng,
        int weight,
        BigDecimal avgCheck,
        Instant updatedAt
) {
}
