package ru.tbank.tmap.transaction.domain;

import ru.tbank.tmap.venue.api.VenueCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Transaction(
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
    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(venueId, "venueId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, got " + amount);
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("lat must be in [-90, 90], got " + lat);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("lng must be in [-180, 180], got " + lng);
        }
    }
}
