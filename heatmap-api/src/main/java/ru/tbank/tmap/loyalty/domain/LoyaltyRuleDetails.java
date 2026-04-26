package ru.tbank.tmap.loyalty.domain;

public record LoyaltyRuleDetails(
        LoyaltyRule rule,
        int currentUsages
) {
}
