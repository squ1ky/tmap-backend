package ru.tbank.tmap.generator.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        UUID transactionId,
        UUID venueId,
        BigDecimal amount,
        double lat,
        double lng,
        Instant occurredAt
) {
}
