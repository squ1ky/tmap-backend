package ru.tbank.tmap.loyalty.application.command;

import java.util.UUID;

public record RedeemLoyaltyRuleCommand(
        UUID ownerId,
        UUID ruleId,
        String qrPayload
) {
}
