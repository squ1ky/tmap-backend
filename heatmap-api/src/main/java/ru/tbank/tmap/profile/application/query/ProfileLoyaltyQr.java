package ru.tbank.tmap.profile.application.query;

import java.util.UUID;

public record ProfileLoyaltyQr(
        UUID userId,
        String qrPayload
) {
}
