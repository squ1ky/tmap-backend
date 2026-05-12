package ru.tbank.tmap.profile.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProfileLoyaltyHistoryProjection {

    UUID getId();

    UUID getVenueId();

    String getVenueName();

    UUID getRuleId();

    String getRuleDescription();

    Integer getDiscountApplied();

    OffsetDateTime getVerifiedAt();
}
