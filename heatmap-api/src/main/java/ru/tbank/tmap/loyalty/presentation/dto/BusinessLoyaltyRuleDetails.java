package ru.tbank.tmap.loyalty.presentation.dto;

import ru.tbank.tmap.loyalty.domain.LoyaltyRule;

public record BusinessLoyaltyRuleDetails(
        LoyaltyRule rule,
        long currentUsages
) {
}
