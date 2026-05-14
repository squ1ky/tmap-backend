package ru.tbank.tmap.transaction.infrastructure.db;

import ru.tbank.tmap.venue.api.VenueCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record TransactionRow(
        UUID id,
        UUID venueId,
        BigDecimal amount,
        double lat,
        double lng,
        long h3Res7,
        long h3Res8,
        long h3Res9,
        VenueCategory category,
        Instant occurredAt
) {
}
