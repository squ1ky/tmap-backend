package ru.tbank.tmap.loyalty.application.command;

import java.util.UUID;

public record ActivateLoyaltyRuleCommand(
        UUID ownerId,
        UUID ruleId,
        UUID userId,
        UUID venueId
) {
}
