package ru.tbank.tmap.transaction.domain;

import ru.tbank.tmap.venue.api.VenueCategory;

import java.math.BigDecimal;
import java.time.Instant;
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
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (venueId == null) {
            throw new IllegalArgumentException("venueId must not be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, got " + amount);
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("lat must be in [-90, 90], got " + lat);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("lng must be in [-180, 180], got " + lng);
        }
    }
}
