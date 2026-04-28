package ru.tbank.tmap.loyalty.exception;

public final class LoyaltyRuleUpdateValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String NO_FIELDS_FOR_UPDATE = "At least one field must be provided for update";

    private LoyaltyRuleUpdateValidationException(final String message) {
        super(message);
    }

    public static LoyaltyRuleUpdateValidationException noFieldsForUpdate() {
        return new LoyaltyRuleUpdateValidationException(NO_FIELDS_FOR_UPDATE);
    }
}
