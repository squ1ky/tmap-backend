package ru.tbank.tmap.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        UUID transactionId,
        UUID venueId,
        BigDecimal amount,
        double lat,
        double lng,
        String category,
        Instant occurredAt
) {
}
