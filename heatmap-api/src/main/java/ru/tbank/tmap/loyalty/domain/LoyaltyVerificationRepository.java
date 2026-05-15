package ru.tbank.tmap.loyalty.domain;

import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LoyaltyVerificationRepository {

    LoyaltyVerification save(LoyaltyVerification loyaltyVerification);

    long countByRuleId(UUID ruleId);

    boolean existsByRuleIdAndUserId(UUID ruleId, UUID userId);

    List<LoyaltyRuleUsageCount> countUsagesByRuleIds(Collection<UUID> ruleIds);
}
