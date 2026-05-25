package ru.tbank.tmap.loyalty.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessLoyaltyHistoryProjection(
        UUID id,
        UUID venueId,
        UUID userId,
        UUID ruleId,
        Integer discountApplied,
        OffsetDateTime verifiedAt
) {
}
