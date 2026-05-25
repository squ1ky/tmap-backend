package ru.tbank.tmap.loyalty.domain.exception;

import java.util.UUID;

public class LoyaltyRuleNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String MESSAGE = "Loyalty rule not found";

    private final UUID ruleId;

    public LoyaltyRuleNotFoundException(final UUID ruleId) {
        super(MESSAGE);
        this.ruleId = ruleId;
    }

    public UUID getRuleId() {
        return ruleId;
    }
}
