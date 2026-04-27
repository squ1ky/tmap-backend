package ru.tbank.tmap.loyalty.business;

import java.math.BigDecimal;

public record BusinessLoyaltyRuleCreateCommand(
        String description,
        BigDecimal discountPercent,
        int maxUsages
) {
}
