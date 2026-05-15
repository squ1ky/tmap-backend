package ru.tbank.tmap.loyalty.application.query;

import ru.tbank.tmap.loyalty.domain.LoyaltyActivationStatus;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;

public record LoyaltyActivationResult(
        LoyaltyActivationStatus status,
        LoyaltyVerification verification
) {
}
