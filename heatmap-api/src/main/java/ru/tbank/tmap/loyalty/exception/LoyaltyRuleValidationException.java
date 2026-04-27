package ru.tbank.tmap.loyalty.exception;

public class LoyaltyRuleValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String NO_FIELDS_FOR_UPDATE = "At least one field must be provided for update";
    private static final String DESCRIPTION_BLANK = "description must not be blank";
    private static final String MAX_USAGES_POSITIVE = "maxUsages must be greater than 0";

    public LoyaltyRuleValidationException(final String message) {
        super(message);
    }

    public static LoyaltyRuleValidationException noFieldsForUpdate() {
        return new LoyaltyRuleValidationException(NO_FIELDS_FOR_UPDATE);
    }

    public static LoyaltyRuleValidationException blankDescription() {
        return new LoyaltyRuleValidationException(DESCRIPTION_BLANK);
    }

    public static LoyaltyRuleValidationException descriptionTooLong(final int maxDescriptionLength) {
        return new LoyaltyRuleValidationException(
                "description must be at most " + maxDescriptionLength + " characters"
        );
    }

    public static LoyaltyRuleValidationException invalidDiscountPercent(
            final int minDiscountPercent,
            final int maxDiscountPercent
    ) {
        return new LoyaltyRuleValidationException(
                "discountPercent must be in [" + minDiscountPercent + ", " + maxDiscountPercent + "]"
        );
    }

    public static LoyaltyRuleValidationException maxUsagesMustBeGreaterThanZero() {
        return new LoyaltyRuleValidationException(MAX_USAGES_POSITIVE);
    }
}
