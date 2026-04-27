package ru.tbank.tmap.loyalty.business;

import java.math.BigDecimal;

public record BusinessLoyaltyRuleUpdateCommand(
        String description,
        BigDecimal discountPercent,
        Integer maxUsages,
        Boolean active
) {
    public boolean hasChanges() {
        return description != null || discountPercent != null || maxUsages != null || active != null;
    }
}
