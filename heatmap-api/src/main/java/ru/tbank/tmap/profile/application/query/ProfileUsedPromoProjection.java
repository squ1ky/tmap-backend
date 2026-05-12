package ru.tbank.tmap.profile.application.query;

import java.time.OffsetDateTime;

public record ProfileUsedPromoProjection(
        String venueName,
        String description,
        Integer discountPercent,
        OffsetDateTime usedAt
) {
}
