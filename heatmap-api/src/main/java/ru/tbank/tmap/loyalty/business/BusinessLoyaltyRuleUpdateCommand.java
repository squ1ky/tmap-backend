package ru.tbank.tmap.loyalty.business;

public record BusinessLoyaltyRuleUpdateCommand(
        String description,
        Integer discountPercent,
        Integer maxUsages,
        Boolean active
) {
    public boolean hasChanges() {
        return description != null || discountPercent != null || maxUsages != null || active != null;
    }
}
