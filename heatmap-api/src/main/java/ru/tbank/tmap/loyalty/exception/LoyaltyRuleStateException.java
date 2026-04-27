package ru.tbank.tmap.loyalty.exception;

import java.util.UUID;

public class LoyaltyRuleStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String INACTIVE_RULE_CANNOT_BE_UPDATED = "Inactive loyalty rule cannot be updated";
    private static final String MAX_USAGES_BELOW_CURRENT = "maxUsages cannot be less than current usages";

    private final UUID ruleId;

    public LoyaltyRuleStateException(final UUID ruleId, final String message) {
        super(message);
        this.ruleId = ruleId;
    }

    public static LoyaltyRuleStateException inactiveRuleCannotBeUpdated(final UUID ruleId) {
        return new LoyaltyRuleStateException(ruleId, INACTIVE_RULE_CANNOT_BE_UPDATED);
    }

    public static LoyaltyRuleStateException maxUsagesCannotBeLessThanCurrentUsages(final UUID ruleId) {
        return new LoyaltyRuleStateException(ruleId, MAX_USAGES_BELOW_CURRENT);
    }

    public UUID getRuleId() {
        return ruleId;
    }
}
