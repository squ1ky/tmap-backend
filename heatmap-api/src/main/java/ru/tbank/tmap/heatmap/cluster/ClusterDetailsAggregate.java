package ru.tbank.tmap.heatmap.cluster;

import ru.tbank.tmap.shared.geo.H3Resolution;

import java.math.BigDecimal;
import java.time.Instant;

public record ClusterDetailsAggregate(
        long h3Index,
        H3Resolution resolution,
        String districtName,
        String districtImageUrl,
        String category,
        Instant hourBucket,
        int txCount,
        BigDecimal avgCheck,
        BigDecimal sumAmount,
        Instant updatedAt
) {
}
