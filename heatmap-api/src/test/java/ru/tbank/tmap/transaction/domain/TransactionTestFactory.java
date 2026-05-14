package ru.tbank.tmap.transaction.domain;

import ru.tbank.tmap.venue.api.VenueCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransactionTestFactory {

    public static final double DEFAULT_LAT = 55.7900;
    public static final double DEFAULT_LNG = 49.1200;
    public static final long DEFAULT_H3_RES7 = 608733123456780000L;
    public static final long DEFAULT_H3_RES8 = 613233123456780000L;
    public static final long DEFAULT_H3_RES9 = 617733123456780000L;
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("100.50");
    public static final VenueCategory DEFAULT_CATEGORY = VenueCategory.FOOD;
    public static final Instant DEFAULT_OCCURRED_AT = Instant.parse("2025-01-15T12:00:00Z");

    private TransactionTestFactory() {
    }

    public static Builder transaction() {
        return new Builder();
    }

    public static final class Builder {

        private UUID id = UUID.randomUUID();
        private UUID venueId = UUID.randomUUID();
        private BigDecimal amount = DEFAULT_AMOUNT;
        private double lat = DEFAULT_LAT;
        private double lng = DEFAULT_LNG;
        private long h3Res7 = DEFAULT_H3_RES7;
        private long h3Res8 = DEFAULT_H3_RES8;
        private long h3Res9 = DEFAULT_H3_RES9;
        private VenueCategory category = DEFAULT_CATEGORY;
        private Instant occurredAt = DEFAULT_OCCURRED_AT;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withVenueId(final UUID venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder withAmount(final BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder withLocation(final double lat, final double lng) {
            this.lat = lat;
            this.lng = lng;
            return this;
        }

        public Builder withH3Indices(final long h3Res7, final long h3Res8, final long h3Res9) {
            this.h3Res7 = h3Res7;
            this.h3Res8 = h3Res8;
            this.h3Res9 = h3Res9;
            return this;
        }

        public Builder withCategory(final VenueCategory category) {
            this.category = category;
            return this;
        }

        public Builder withOccurredAt(final Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Transaction build() {
            return new Transaction(
                    id,
                    venueId,
                    amount,
                    lat,
                    lng,
                    h3Res7,
                    h3Res8,
                    h3Res9,
                    category,
                    occurredAt
            );
        }
    }
}
