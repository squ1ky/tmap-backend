package ru.tbank.tmap.profile.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileLoyaltyHistoryProjection(
        UUID id,
        UUID venueId,
        String venueName,
        UUID ruleId,
        String ruleDescription,
        Integer discountApplied,
        OffsetDateTime verifiedAt
) {
}
