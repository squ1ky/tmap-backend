package ru.tbank.tmap.loyalty.application.command;

import java.util.UUID;

public record IssueLoyaltyQrCommand(
        UUID userId,
        UUID venueId,
        UUID ruleId
) {
}
