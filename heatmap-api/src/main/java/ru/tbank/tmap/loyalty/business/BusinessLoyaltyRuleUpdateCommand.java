package ru.tbank.tmap.loyalty.business;

import ru.tbank.tmap.loyalty.business.validation.AtLeastOneFieldPresent;

@AtLeastOneFieldPresent
public record BusinessLoyaltyRuleUpdateCommand(
        String description,
        Integer discountPercent,
        Integer maxUsages,
        Boolean active
) {
}
