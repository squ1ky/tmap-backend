package ru.tbank.tmap.heatmap.application.query;

import ru.tbank.tmap.shared.geo.H3Resolution;

import java.math.BigDecimal;
import java.time.Instant;

public record ClusterDetailsAggregate(
        long h3Index,
        H3Resolution resolution,
        String districtName,
        String districtImageUrl,
        Instant hourBucket,
        int txCount,
        BigDecimal avgCheck,
        BigDecimal sumAmount,
        Instant updatedAt,
        boolean isAnomaly,
        BigDecimal anomalyRatio,
        BigDecimal baselineAvg
) {
    public ClusterDetailsAggregate withDistrictImageUrl(final String newDistrictImageUrl) {
        return new ClusterDetailsAggregate(
                h3Index,
                resolution,
                districtName,
                newDistrictImageUrl,
                hourBucket,
                txCount,
                avgCheck,
                sumAmount,
                updatedAt,
                isAnomaly,
                anomalyRatio,
                baselineAvg
        );
    }
}
