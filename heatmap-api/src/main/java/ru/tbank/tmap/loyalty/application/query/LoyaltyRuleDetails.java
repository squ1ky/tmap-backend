package ru.tbank.tmap.loyalty.application.query;

import ru.tbank.tmap.loyalty.domain.LoyaltyRule;

public record LoyaltyRuleDetails(
        LoyaltyRule rule,
        long currentUsages
) {
}
