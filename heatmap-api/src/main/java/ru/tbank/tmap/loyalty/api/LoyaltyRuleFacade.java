package ru.tbank.tmap.loyalty.api;

import java.util.List;
import java.util.UUID;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;

public interface LoyaltyRuleFacade {

    List<LoyaltyRuleDetails> getActiveVenueRules(UUID venueId);
}
