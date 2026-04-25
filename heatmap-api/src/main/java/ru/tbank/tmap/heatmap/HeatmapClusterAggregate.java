package ru.tbank.tmap.heatmap;

import java.math.BigDecimal;
import java.time.Instant;

public record HeatmapClusterAggregate(
        long h3Index,
        double centerLat,
        double centerLng,
        int txCount,
        BigDecimal avgCheck,
        BigDecimal sumAmount,
        Instant updatedAt
) {
}
