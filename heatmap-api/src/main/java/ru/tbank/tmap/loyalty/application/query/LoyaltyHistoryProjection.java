package ru.tbank.tmap.loyalty.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LoyaltyHistoryProjection(
        UUID id,
        UUID venueId,
        String venueName,
        String venueCategory,
        UUID ruleId,
        String ruleDescription,
        Integer discountApplied,
        OffsetDateTime verifiedAt
) {
}
