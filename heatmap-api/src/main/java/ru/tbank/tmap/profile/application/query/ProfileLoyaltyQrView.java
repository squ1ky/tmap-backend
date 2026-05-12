package ru.tbank.tmap.profile.application.query;

import java.util.UUID;

public record ProfileLoyaltyQrView(
        UUID userId,
        String qrPayload
) {
}
