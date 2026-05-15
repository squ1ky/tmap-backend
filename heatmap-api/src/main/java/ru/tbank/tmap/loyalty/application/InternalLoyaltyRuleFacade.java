package ru.tbank.tmap.loyalty.application;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.api.LoyaltyRuleFacade;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalLoyaltyRuleFacade implements LoyaltyRuleFacade {

    private final LoyaltyRuleService loyaltyRuleService;

    @Override
    public List<LoyaltyRuleDetails> getActiveVenueRules(final UUID venueId) {
        return loyaltyRuleService.getActiveVenueRules(venueId);
    }
}
