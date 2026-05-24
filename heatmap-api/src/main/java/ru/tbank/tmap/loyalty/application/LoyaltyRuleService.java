package ru.tbank.tmap.loyalty.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyRuleService {

    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final LoyaltyVerificationRepository loyaltyVerificationRepository;

    public List<LoyaltyRuleDetails> getActiveVenueRules(final UUID venueId) {
        final List<LoyaltyRule> rules = loyaltyRuleRepository
                .findByVenueIdAndActiveTrueOrderByCreatedAtDescIdDesc(venueId);
        return toRuleDetails(rules);
    }

    public List<LoyaltyRuleDetails> getAvailableVenueRulesForUser(final UUID venueId, final UUID userId) {
        final List<LoyaltyRule> rules = loyaltyRuleRepository
                .findByVenueIdAndActiveTrueOrderByCreatedAtDescIdDesc(venueId);
        if (rules.isEmpty()) {
            return List.of();
        }

        final Set<UUID> usedRuleIds = Set.copyOf(
                loyaltyVerificationRepository.findUsedRuleIdsByUserIdAndRuleIds(userId, extractRuleIds(rules))
        );

        return toRuleDetails(rules.stream()
                .filter(rule -> !usedRuleIds.contains(rule.getId()))
                .toList());
    }

    private List<LoyaltyRuleDetails> toRuleDetails(final List<LoyaltyRule> rules) {
        if (rules.isEmpty()) {
            return List.of();
        }

        final List<UUID> ruleIds = extractRuleIds(rules);
        final Map<UUID, Long> usagesByRuleId = loyaltyVerificationRepository.countUsagesByRuleIds(ruleIds).stream()
                .collect(Collectors.toMap(
                        LoyaltyRuleUsageCount::ruleId,
                        LoyaltyRuleUsageCount::usages
                ));

        return rules.stream()
                .map(rule -> new LoyaltyRuleDetails(rule, usagesByRuleId.getOrDefault(rule.getId(), 0L)))
                .toList();
    }

    private List<UUID> extractRuleIds(final List<LoyaltyRule> rules) {
        return rules.stream()
                .map(LoyaltyRule::getId)
                .toList();
    }
}
