package ru.tbank.tmap.loyalty.domain;

import ru.tbank.tmap.loyalty.repository.LoyaltyRuleUsageCount;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LoyaltyVerificationRepository {

    LoyaltyVerification save(LoyaltyVerification loyaltyVerification);

    long countByRuleId(UUID ruleId);

    List<LoyaltyRuleUsageCount> countUsagesByRuleIds(Collection<UUID> ruleIds);
}
