package ru.tbank.tmap.loyalty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "loyalty_qr_sessions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class LoyaltyQrSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 255)
    private String tokenHash;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "venue_id", nullable = false, updatable = false)
    private UUID venueId;

    @Column(name = "rule_id", nullable = false, updatable = false)
    private UUID ruleId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public LoyaltyQrSession(
            final UUID id,
            final String tokenHash,
            final UUID userId,
            final UUID venueId,
            final UUID ruleId,
            final OffsetDateTime expiresAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.venueId = Objects.requireNonNull(venueId, "venueId");
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }
}
