package ru.tbank.tmap.loyalty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
import ru.tbank.tmap.venue.domain.Venue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "loyalty_rules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class LoyaltyRule {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @NotBlank
    @Size(max = 255)
    @Column(name = "description", nullable = false, length = 255)
    @ToString.Include
    private String description;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    @ToString.Include
    private BigDecimal discountPercent;

    @Positive
    @Column(name = "max_usages", nullable = false)
    @ToString.Include
    private int maxUsages;

    @Column(name = "active", nullable = false)
    @ToString.Include
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public LoyaltyRule(UUID id,
                       Venue venue,
                       String description,
                       BigDecimal discountPercent,
                       int maxUsages) {
        this.id = Objects.requireNonNull(id, "id");
        this.venue = Objects.requireNonNull(venue, "venue");
        this.description = Objects.requireNonNull(description, "description");
        Objects.requireNonNull(discountPercent, "discountPercent");
        if (discountPercent.signum() < 0 || discountPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("discountPercent must be in [0, 100], got " + discountPercent);
        }
        if (maxUsages <= 0) {
            throw new IllegalArgumentException("maxUsages must be > 0, got " + maxUsages);
        }
        this.discountPercent = discountPercent;
        this.maxUsages = maxUsages;
    }
}
