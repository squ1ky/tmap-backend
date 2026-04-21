package ru.tbank.tmap.repository.model;

import ru.tbank.tmap.domain.geo.H3Resolution;

import java.math.BigDecimal;
import java.time.Instant;

public record ClusterDetailsAggregate(
        long h3Index,
        H3Resolution resolution,
        int txCount,
        BigDecimal avgCheck,
        BigDecimal sumAmount,
        Instant updatedAt
) {
}
