package ru.tbank.tmap.loyalty.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LoyaltyQrView(
        UUID venueId,
        UUID ruleId,
        String qrPayload,
        OffsetDateTime expiresAt
) {
}
