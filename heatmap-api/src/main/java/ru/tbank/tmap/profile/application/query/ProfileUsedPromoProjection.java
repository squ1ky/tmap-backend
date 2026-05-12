package ru.tbank.tmap.profile.application.query;

import java.time.OffsetDateTime;

public interface ProfileUsedPromoProjection {

    String getVenueName();

    String getDescription();

    Integer getDiscountPercent();

    OffsetDateTime getUsedAt();
}
