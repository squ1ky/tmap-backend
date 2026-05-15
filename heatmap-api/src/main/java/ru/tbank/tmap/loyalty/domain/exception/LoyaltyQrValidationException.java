package ru.tbank.tmap.loyalty.domain.exception;

public final class LoyaltyQrValidationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private static final String RULE_DOES_NOT_BELONG_TO_REQUESTED_VENUE =
            "Rule does not belong to requested venue";
    private static final String INACTIVE_RULE_CANNOT_GENERATE_QR =
            "Loyalty QR can be generated only for active rules";
    private static final String INVALID_QR = "Loyalty QR is invalid";
    private static final String USED_QR = "Loyalty QR has already been used";
    private static final String EXPIRED_QR = "Loyalty QR is expired";
    private static final String MISSING_QR_PAYLOAD = "Loyalty QR payload is missing";
    private static final String INVALID_QR_PAYLOAD_FORMAT = "Loyalty QR payload format is invalid";
    private static final String QR_DOES_NOT_BELONG_TO_REQUESTED_RULE =
            "QR does not belong to requested loyalty rule";
    private static final String QR_DOES_NOT_BELONG_TO_REQUESTED_VENUE =
            "QR does not belong to requested venue";

    private LoyaltyQrValidationException(final String message) {
        super(message);
    }

    public static LoyaltyQrValidationException ruleDoesNotBelongToRequestedVenue() {
        return new LoyaltyQrValidationException(RULE_DOES_NOT_BELONG_TO_REQUESTED_VENUE);
    }

    public static LoyaltyQrValidationException inactiveRuleCannotGenerateQr() {
        return new LoyaltyQrValidationException(INACTIVE_RULE_CANNOT_GENERATE_QR);
    }

    public static LoyaltyQrValidationException invalidQr() {
        return new LoyaltyQrValidationException(INVALID_QR);
    }

    public static LoyaltyQrValidationException usedQr() {
        return new LoyaltyQrValidationException(USED_QR);
    }

    public static LoyaltyQrValidationException expiredQr() {
        return new LoyaltyQrValidationException(EXPIRED_QR);
    }

    public static LoyaltyQrValidationException missingQrPayload() {
        return new LoyaltyQrValidationException(MISSING_QR_PAYLOAD);
    }

    public static LoyaltyQrValidationException invalidQrPayloadFormat() {
        return new LoyaltyQrValidationException(INVALID_QR_PAYLOAD_FORMAT);
    }

    public static LoyaltyQrValidationException qrDoesNotBelongToRequestedRule() {
        return new LoyaltyQrValidationException(QR_DOES_NOT_BELONG_TO_REQUESTED_RULE);
    }

    public static LoyaltyQrValidationException qrDoesNotBelongToRequestedVenue() {
        return new LoyaltyQrValidationException(QR_DOES_NOT_BELONG_TO_REQUESTED_VENUE);
    }
}
