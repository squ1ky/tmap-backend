package ru.tbank.tmap.loyalty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "description", nullable = false, length = 255)
    @ToString.Include
    private String description;

    @Min(0)
    @Max(100)
    @Column(name = "discount_percent", nullable = false)
    @ToString.Include
    private int discountPercent;

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
                       UUID venueId,
                       String description,
                       int discountPercent,
                       int maxUsages) {
        this.id = Objects.requireNonNull(id, "id");
        this.venueId = Objects.requireNonNull(venueId, "venueId");
        this.description = Objects.requireNonNull(description, "description");
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("discountPercent must be in [0, 100], got " + discountPercent);
        }
        if (maxUsages <= 0) {
            throw new IllegalArgumentException("maxUsages must be > 0, got " + maxUsages);
        }
        this.discountPercent = discountPercent;
        this.maxUsages = maxUsages;
    }
}
