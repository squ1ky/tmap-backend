package ru.tbank.tmap.loyalty.exception;

import java.util.UUID;

public class LoyaltyRuleNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final UUID ruleId;

    public LoyaltyRuleNotFoundException(final UUID ruleId) {
        super("Loyalty rule not found");
        this.ruleId = ruleId;
    }

    public UUID getRuleId() {
        return ruleId;
    }
}
