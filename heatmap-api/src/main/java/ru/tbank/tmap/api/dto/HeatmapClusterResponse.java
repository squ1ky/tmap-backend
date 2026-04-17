package ru.tbank.tmap.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HeatmapClusterResponse(
        String h3Index,
        double centerLat,
        double centerLng,
        int txCount,
        BigDecimal avgCheck,
        BigDecimal sumAmount,
        Instant updatedAt
) {
}
