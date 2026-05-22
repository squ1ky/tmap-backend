package ru.tbank.tmap.loyalty.api;

import java.util.UUID;

public interface LoyaltyVenueFacade {

    void deleteVerificationHistory(UUID venueId);
}
