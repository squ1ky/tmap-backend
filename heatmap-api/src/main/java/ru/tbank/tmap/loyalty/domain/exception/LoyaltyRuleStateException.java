package ru.tbank.tmap.loyalty.domain.exception;

import java.util.UUID;

public class LoyaltyRuleStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String ACTIVE_RULE_CANNOT_BE_UPDATED = "Active loyalty rule cannot be updated";
    private static final String MAX_USAGES_BELOW_CURRENT = "maxUsages cannot be less than current usages";
    private static final String RULE_IS_INACTIVE = "Loyalty rule is inactive";

    private final UUID ruleId;

    public LoyaltyRuleStateException(final UUID ruleId, final String message) {
        super(message);
        this.ruleId = ruleId;
    }

    public static LoyaltyRuleStateException activeRuleCannotBeUpdated(final UUID ruleId) {
        return new LoyaltyRuleStateException(ruleId, ACTIVE_RULE_CANNOT_BE_UPDATED);
    }

    public static LoyaltyRuleStateException maxUsagesCannotBeLessThanCurrentUsages(final UUID ruleId) {
        return new LoyaltyRuleStateException(ruleId, MAX_USAGES_BELOW_CURRENT);
    }

    public static LoyaltyRuleStateException inactiveRuleCannotBeActivated(final UUID ruleId) {
        return new LoyaltyRuleStateException(ruleId, RULE_IS_INACTIVE);
    }

    public UUID getRuleId() {
        return ruleId;
    }
}
