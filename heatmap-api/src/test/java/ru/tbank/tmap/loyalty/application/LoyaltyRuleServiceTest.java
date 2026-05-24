package ru.tbank.tmap.loyalty.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;

@ExtendWith(MockitoExtension.class)
class LoyaltyRuleServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FIRST_RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SECOND_RULE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private LoyaltyRuleRepository loyaltyRuleRepository;

    @Mock
    private LoyaltyVerificationRepository loyaltyVerificationRepository;

    private LoyaltyRuleService loyaltyRuleService;

    @BeforeEach
    void setUp() {
        loyaltyRuleService = new LoyaltyRuleService(loyaltyRuleRepository, loyaltyVerificationRepository);
    }

    @Test
    void getAvailableVenueRulesForUser_whenUserUsedPartOfRules_thenReturnsOnlyUnusedRules() {
        final LoyaltyRule firstRule = loyaltyRule(FIRST_RULE_ID, "Discount 15%", 15, 100);
        final LoyaltyRule secondRule = loyaltyRule(SECOND_RULE_ID, "Coffee 5%", 5, 20);

        given(loyaltyRuleRepository.findByVenueIdAndActiveTrueOrderByCreatedAtDescIdDesc(VENUE_ID))
                .willReturn(List.of(firstRule, secondRule));
        given(loyaltyVerificationRepository.findUsedRuleIdsByUserIdAndRuleIds(
                USER_ID,
                List.of(FIRST_RULE_ID, SECOND_RULE_ID)))
                .willReturn(List.of(FIRST_RULE_ID));
        given(loyaltyVerificationRepository.countUsagesByRuleIds(List.of(SECOND_RULE_ID)))
                .willReturn(List.of(usageCount(SECOND_RULE_ID, 3L)));

        final List<LoyaltyRuleDetails> result = loyaltyRuleService.getAvailableVenueRulesForUser(VENUE_ID, USER_ID);

        assertThat(result)
                .extracting(details -> details.rule().getId(), LoyaltyRuleDetails::currentUsages)
                .containsExactly(tuple(SECOND_RULE_ID, 3L));
    }

    @Test
    void getAvailableVenueRulesForUser_whenNoRulesExist_thenReturnsEmptyList() {
        given(loyaltyRuleRepository.findByVenueIdAndActiveTrueOrderByCreatedAtDescIdDesc(VENUE_ID))
                .willReturn(List.of());

        final List<LoyaltyRuleDetails> result = loyaltyRuleService.getAvailableVenueRulesForUser(VENUE_ID, USER_ID);

        assertThat(result).isEmpty();
    }

    private LoyaltyRule loyaltyRule(
            final UUID ruleId,
            final String description,
            final int discountPercent,
            final int maxUsages
    ) {
        final LoyaltyRule rule = new LoyaltyRule(ruleId, VENUE_ID, description, discountPercent, maxUsages);
        rule.setCreatedAt(OffsetDateTime.parse("2026-05-01T10:00:00+03:00"));
        return rule;
    }

    private LoyaltyRuleUsageCount usageCount(final UUID ruleId, final long usages) {
        return new LoyaltyRuleUsageCount(ruleId, usages);
    }
}
