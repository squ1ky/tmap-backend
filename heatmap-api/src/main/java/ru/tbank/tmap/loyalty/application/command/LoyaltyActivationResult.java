package ru.tbank.tmap.loyalty.application.command;

import org.openapitools.model.LoyaltyActivationStatus;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;

public record LoyaltyActivationResult(
        LoyaltyActivationStatus status,
        LoyaltyVerification verification
) {
}
