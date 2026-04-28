package ru.tbank.tmap.loyalty.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.exception.LoyaltyRuleStateException;
import ru.tbank.tmap.loyalty.repository.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.repository.LoyaltyVerificationRepository;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.user.UserRole;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class BusinessLoyaltyRuleServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private LoyaltyRuleRepository loyaltyRuleRepository;

    @Mock
    private LoyaltyVerificationRepository loyaltyVerificationRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    private BusinessLoyaltyRuleService businessLoyaltyRuleService;

    @BeforeEach
    void setUp() {
        businessLoyaltyRuleService = new BusinessLoyaltyRuleService(
                loyaltyRuleRepository,
                loyaltyVerificationRepository,
                venueRepository,
                userRepository,
                new BusinessLoyaltyRuleMapper()
        );
    }

    @Test
    void createRule_whenVenueBelongsToOwner_thenCreatesRule() {
        final Venue venue = venue();
        final LoyaltyRule unsavedRule = loyaltyRule("Discount 15%", 15, 100, true);
        final BusinessLoyaltyRuleCreateCommand command = new BusinessLoyaltyRuleCreateCommand(
                "Discount 15%",
                15,
                100
        );
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(venue));
        given(loyaltyRuleRepository.save(org.mockito.ArgumentMatchers.any(LoyaltyRule.class))).willReturn(unsavedRule);

        final BusinessLoyaltyRuleDetails result = businessLoyaltyRuleService.createRule(OWNER_EMAIL, VENUE_ID, command);

        assertThat(result.rule().getVenue().getId()).isEqualTo(VENUE_ID);
        assertThat(result.rule().getDescription()).isEqualTo("Discount 15%");
        assertThat(result.rule().getDiscountPercent()).isEqualTo(15);
        assertThat(result.rule().getMaxUsages()).isEqualTo(100);
        assertThat(result.currentUsages()).isZero();
        assertThat(result.rule().isActive()).isTrue();
    }

    @Test
    void createRule_whenVenueDoesNotBelongToOwner_thenThrowsNotFound() {
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> businessLoyaltyRuleService.createRule(
                OWNER_EMAIL,
                VENUE_ID,
                new BusinessLoyaltyRuleCreateCommand("Discount", 10, 10)
        )).isInstanceOf(VenueNotFoundException.class);
    }

    @Test
    void getVenueRules_whenVenueBelongsToOwner_thenReturnsRulesWithUsageCounts() {
        final LoyaltyRule firstRule = loyaltyRule("Discount 15%", 15, 100, true);
        firstRule.setCreatedAt(OffsetDateTime.parse("2026-04-27T09:00:00+03:00"));
        final LoyaltyRule secondRule = new LoyaltyRule(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                venue(),
                "Coffee 5%",
                5,
                20
        );
        secondRule.setCreatedAt(OffsetDateTime.parse("2026-04-26T09:00:00+03:00"));
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(venue()));
        given(loyaltyRuleRepository.findByVenueIdAndVenueOwnerIdOrderByCreatedAtDescIdDesc(VENUE_ID, OWNER_ID))
                .willReturn(List.of(firstRule, secondRule));
        given(loyaltyVerificationRepository.countUsagesByRuleIds(List.of(RULE_ID, secondRule.getId())))
                .willReturn(List.of(
                        usageCount(RULE_ID, 7L),
                        usageCount(secondRule.getId(), 2L)
                ));

        final List<BusinessLoyaltyRuleDetails> result = businessLoyaltyRuleService.getVenueRules(OWNER_EMAIL, VENUE_ID);

        assertThat(result)
                .extracting(details -> details.rule().getId(), BusinessLoyaltyRuleDetails::currentUsages)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(RULE_ID, 7L),
                        org.assertj.core.groups.Tuple.tuple(secondRule.getId(), 2L)
                );
    }

    @Test
    void getRuleById_whenRuleBelongsToOwner_thenReturnsRule() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, true);
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(loyaltyRuleRepository.findByIdAndVenueOwnerId(RULE_ID, OWNER_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(9L);

        final Optional<BusinessLoyaltyRuleDetails> result = businessLoyaltyRuleService.getRuleById(OWNER_EMAIL, RULE_ID);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().currentUsages()).isEqualTo(9);
    }

    @Test
    void updateRule_whenRuleBelongsToAnotherOwner_thenThrowsNotFound() {
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(loyaltyRuleRepository.findByIdAndVenueOwnerId(RULE_ID, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> businessLoyaltyRuleService.updateRule(
                OWNER_EMAIL,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand("New title", null, null, null)
        )).isInstanceOf(LoyaltyRuleNotFoundException.class);
    }

    @Test
    void updateRule_whenRuleIsInactive_thenAllowsEditing() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, false);
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(loyaltyRuleRepository.findByIdAndVenueOwnerId(RULE_ID, OWNER_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(4L);

        final BusinessLoyaltyRuleDetails result = businessLoyaltyRuleService.updateRule(
                OWNER_EMAIL,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand("New title", null, null, null)
        );

        assertThat(result.rule().getDescription()).isEqualTo("New title");
        assertThat(result.rule().isActive()).isFalse();
        assertThat(result.currentUsages()).isEqualTo(4L);
    }

    @Test
    void updateRule_whenMaxUsagesBelowCurrentUsages_thenThrowsConflict() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, false);
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(loyaltyRuleRepository.findByIdAndVenueOwnerId(RULE_ID, OWNER_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(6L);

        assertThatThrownBy(() -> businessLoyaltyRuleService.updateRule(
                OWNER_EMAIL,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand(null, null, 5, null)
        )).isInstanceOf(LoyaltyRuleStateException.class)
                .hasMessage("maxUsages cannot be less than current usages");
    }

    @Test
    void updateRule_whenRuleIsActive_thenThrowsConflict() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, true);
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(loyaltyRuleRepository.findByIdAndVenueOwnerId(RULE_ID, OWNER_ID)).willReturn(Optional.of(rule));

        assertThatThrownBy(() -> businessLoyaltyRuleService.updateRule(
                OWNER_EMAIL,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand("Discount 20%", 20, 120, false)
        )).isInstanceOf(LoyaltyRuleStateException.class)
                .hasMessage("Active loyalty rule cannot be updated");
    }

    @Test
    void updateRule_whenRequestIsValid_thenUpdatesFields() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, false);
        given(userRepository.findByEmail(OWNER_EMAIL)).willReturn(Optional.of(owner()));
        given(loyaltyRuleRepository.findByIdAndVenueOwnerId(RULE_ID, OWNER_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(4L);

        final BusinessLoyaltyRuleDetails result = businessLoyaltyRuleService.updateRule(
                OWNER_EMAIL,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand("Discount 20%", 20, 120, false)
        );

        assertThat(result.rule().getDescription()).isEqualTo("Discount 20%");
        assertThat(result.rule().getDiscountPercent()).isEqualTo(20);
        assertThat(result.rule().getMaxUsages()).isEqualTo(120);
        assertThat(result.currentUsages()).isEqualTo(4);
        assertThat(result.rule().isActive()).isFalse();
    }

    private User owner() {
        return new User(OWNER_ID, OWNER_EMAIL, "password-hash", "Owner", UserRole.BUSINESS_OWNER);
    }

    private Venue venue() {
        final Venue venue = new Venue(
                VENUE_ID,
                owner(),
                "Bar One",
                "Kazan Center, 2",
                GeoPoint.of(55.7905, 49.1140),
                617422037122678783L,
                VenueCategory.ENTERTAINMENT
        );
        venue.setStatus(VenueStatus.ACTIVE);
        return venue;
    }

    private LoyaltyRule loyaltyRule(
            final String description,
            final int discountPercent,
            final int maxUsages,
            final boolean active
    ) {
        final LoyaltyRule rule = new LoyaltyRule(RULE_ID, venue(), description, discountPercent, maxUsages);
        rule.setActive(active);
        rule.setCreatedAt(OffsetDateTime.parse("2026-04-27T08:30:00+03:00"));
        return rule;
    }

    private LoyaltyVerificationRepository.LoyaltyRuleUsageCount usageCount(final UUID ruleId, final long usages) {
        return new LoyaltyVerificationRepository.LoyaltyRuleUsageCount(ruleId, usages);
    }
}
