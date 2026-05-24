package ru.tbank.tmap.loyalty.application.query;

import java.util.UUID;
import ru.tbank.tmap.loyalty.domain.LoyaltyActivationStatus;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;

public record LoyaltyActivationResult(
        UUID ruleId,
        LoyaltyActivationStatus status,
        LoyaltyVerification verification
) {
}
