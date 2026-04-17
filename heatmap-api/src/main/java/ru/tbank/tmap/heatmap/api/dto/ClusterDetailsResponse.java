package ru.tbank.tmap.heatmap.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ClusterDetailsResponse(
        String h3Index,
        int resolution,
        int txCount,
        BigDecimal avgCheck,
        BigDecimal sumAmount,
        Instant updatedAt
) {
}
