package ru.tbank.tmap.loyalty.business;

import ru.tbank.tmap.loyalty.domain.LoyaltyRule;

public record BusinessLoyaltyRuleDetails(
        LoyaltyRule rule,
        long currentUsages
) {
}
