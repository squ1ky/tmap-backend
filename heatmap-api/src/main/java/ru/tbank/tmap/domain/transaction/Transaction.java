package ru.tbank.tmap.domain.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.tbank.tmap.domain.venue.GeoPoint;
import ru.tbank.tmap.domain.venue.Venue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Transaction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @NotNull
    @Positive
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    @ToString.Include
    private BigDecimal amount;

    @NotNull
    @Embedded
    private GeoPoint location;

    @Column(name = "h3_res7", nullable = false)
    private long h3Res7;

    @Column(name = "h3_res8", nullable = false)
    private long h3Res8;

    @Column(name = "h3_res9", nullable = false)
    private long h3Res9;

    @NotBlank
    @Size(max = 64)
    @Column(name = "category", nullable = false, length = 64)
    @ToString.Include
    private String category;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    @ToString.Include
    private OffsetDateTime occurredAt;

    public Transaction(UUID id,
                       Venue venue,
                       BigDecimal amount,
                       GeoPoint location,
                       long h3Res7,
                       long h3Res8,
                       long h3Res9,
                       String category,
                       OffsetDateTime occurredAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.venue = Objects.requireNonNull(venue, "venue");
        this.amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, got " + amount);
        }
        this.location = Objects.requireNonNull(location, "location");
        this.h3Res7 = h3Res7;
        this.h3Res8 = h3Res8;
        this.h3Res9 = h3Res9;
        this.category = Objects.requireNonNull(category, "category");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
