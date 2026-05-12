package ru.tbank.tmap.loyalty.application.query;

import java.time.OffsetDateTime;

public record UsedPromoProjection(
        String venueName,
        String description,
        Integer discountPercent,
        OffsetDateTime usedAt
) {
}
