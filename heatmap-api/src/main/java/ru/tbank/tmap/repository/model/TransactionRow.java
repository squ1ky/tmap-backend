package ru.tbank.tmap.repository.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRow(
        UUID id,
        UUID venueId,
        BigDecimal amount,
        double lat,
        double lng,
        long h3Res7,
        long h3Res8,
        long h3Res9,
        String category,
        Instant occurredAt
) {
}
