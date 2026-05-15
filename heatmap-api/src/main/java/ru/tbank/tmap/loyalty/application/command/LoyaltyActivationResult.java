package ru.tbank.tmap.loyalty.application.command;

import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;

public record LoyaltyActivationResult(
        LoyaltyActivationStatus status,
        LoyaltyVerification verification
) {
}
