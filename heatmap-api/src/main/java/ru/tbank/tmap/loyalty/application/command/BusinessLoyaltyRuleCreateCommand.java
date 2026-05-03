package ru.tbank.tmap.loyalty.application.command;

public record BusinessLoyaltyRuleCreateCommand(
        String description,
        int discountPercent,
        int maxUsages
) {
}
