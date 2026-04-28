package ru.tbank.tmap.loyalty.business;

public record BusinessLoyaltyRuleCreateCommand(
        String description,
        int discountPercent,
        int maxUsages
) {
}
