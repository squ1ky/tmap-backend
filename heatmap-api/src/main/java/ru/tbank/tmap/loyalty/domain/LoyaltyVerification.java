package ru.tbank.tmap.loyalty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.venue.domain.Venue;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "loyalty_verifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_loyalty_verifications_rule_user",
                columnNames = {"rule_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class LoyaltyVerification {

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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private LoyaltyRule rule;

    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "discount_applied", nullable = false)
    @ToString.Include
    private int discountApplied;

    @Column(name = "verified_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime verifiedAt;

    public LoyaltyVerification(UUID id,
                               Venue venue,
                               User user,
                               LoyaltyRule rule,
                               int discountApplied) {
        this.id = Objects.requireNonNull(id, "id");
        this.venue = Objects.requireNonNull(venue, "venue");
        this.user = Objects.requireNonNull(user, "user");
        this.rule = Objects.requireNonNull(rule, "rule");
        if (discountApplied < 0 || discountApplied > 100) {
            throw new IllegalArgumentException("discountApplied must be in [0, 100]");
        }
        this.discountApplied = discountApplied;
    }
}
