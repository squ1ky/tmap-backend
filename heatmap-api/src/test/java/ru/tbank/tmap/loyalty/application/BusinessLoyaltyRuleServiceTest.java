package ru.tbank.tmap.loyalty.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSession;
import ru.tbank.tmap.loyalty.application.command.RedeemLoyaltyRuleCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleCreateCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleUpdateCommand;
import ru.tbank.tmap.loyalty.application.exception.VenueAccessDeniedException;
import ru.tbank.tmap.loyalty.application.port.VenueOwnershipPort;
import ru.tbank.tmap.loyalty.application.query.LoyaltyActivationResult;
import ru.tbank.tmap.loyalty.domain.LoyaltyActivationStatus;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleStateException;

@ExtendWith(MockitoExtension.class)
class BusinessLoyaltyRuleServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String QR_PAYLOAD = "lqr:1:test-token";
    private static final RedeemLoyaltyRuleCommand REDEEM_COMMAND =
            new RedeemLoyaltyRuleCommand(OWNER_ID, QR_PAYLOAD);

    @Mock
    private LoyaltyRuleRepository loyaltyRuleRepository;

    @Mock
    private LoyaltyVerificationRepository loyaltyVerificationRepository;

    @Mock
    private VenueOwnershipPort venueOwnershipPort;

    @Mock
    private LoyaltyQrService loyaltyQrService;

    private BusinessLoyaltyRuleService businessLoyaltyRuleService;

    @BeforeEach
    void setUp() {
        businessLoyaltyRuleService = new BusinessLoyaltyRuleService(
                loyaltyRuleRepository,
                loyaltyVerificationRepository,
                venueOwnershipPort,
                loyaltyQrService
        );
    }

    @Test
    void createRule_whenVenueBelongsToOwner_thenCreatesRule() {
        final LoyaltyRule savedRule = loyaltyRule(
                "Discount 15%",
                15,
                100,
                true
        );
        final BusinessLoyaltyRuleCreateCommand command = new BusinessLoyaltyRuleCreateCommand(
                "Discount 15%",
                15,
                100
        );

        given(loyaltyRuleRepository.save(any(LoyaltyRule.class))).willReturn(savedRule);

        final LoyaltyRuleDetails result = businessLoyaltyRuleService.createRule(OWNER_ID, VENUE_ID, command);

        assertThat(result.rule().getVenueId()).isEqualTo(VENUE_ID);
        assertThat(result.rule().getDescription()).isEqualTo("Discount 15%");
        assertThat(result.rule().getDiscountPercent()).isEqualTo(15);
        assertThat(result.rule().getMaxUsages()).isEqualTo(100);
        assertThat(result.currentUsages()).isZero();
        assertThat(result.rule().isActive()).isTrue();
    }

    @Test
    void createRule_whenOwnerHasNoAccess_thenThrowsAccessDenied() {
        willThrow(new VenueAccessDeniedException(VENUE_ID)).given(venueOwnershipPort).requireOwner(VENUE_ID, OWNER_ID);

        assertThatThrownBy(() -> businessLoyaltyRuleService.createRule(
                OWNER_ID,
                VENUE_ID,
                new BusinessLoyaltyRuleCreateCommand("Discount", 10, 10)
        )).isInstanceOf(VenueAccessDeniedException.class);
    }

    @Test
    void getVenueRules_whenVenueBelongsToOwner_thenReturnsRulesWithUsageCounts() {
        final LoyaltyRule firstRule = loyaltyRule(
                "Discount 15%",
                15,
                100,
                true
        );
        firstRule.setCreatedAt(OffsetDateTime.parse("2026-04-27T09:00:00+03:00"));
        final UUID secondRuleId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        final LoyaltyRule secondRule = new LoyaltyRule(
                secondRuleId,
                VENUE_ID,
                "Coffee 5%",
                5,
                20
        );
        secondRule.setCreatedAt(OffsetDateTime.parse("2026-04-26T09:00:00+03:00"));

        given(loyaltyRuleRepository.findByVenueIdOrderByCreatedAtDescIdDesc(VENUE_ID))
                .willReturn(List.of(firstRule, secondRule));
        given(loyaltyVerificationRepository.countUsagesByRuleIds(List.of(RULE_ID, secondRuleId)))
                .willReturn(List.of(
                        usageCount(RULE_ID, 7L),
                        usageCount(secondRuleId, 2L)
                ));

        final List<LoyaltyRuleDetails> result = businessLoyaltyRuleService.getVenueRules(OWNER_ID, VENUE_ID);

        assertThat(result)
                .extracting(details -> details.rule().getId(), LoyaltyRuleDetails::currentUsages)
                .containsExactly(
                        tuple(RULE_ID, 7L),
                        tuple(secondRuleId, 2L)
                );
    }

    @Test
    void getRuleById_whenRuleBelongsToOwner_thenReturnsRule() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, true);

        given(loyaltyRuleRepository.findById(RULE_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(9L);

        final Optional<LoyaltyRuleDetails> result = businessLoyaltyRuleService.getRuleById(OWNER_ID, RULE_ID);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().currentUsages()).isEqualTo(9);
    }

    @Test
    void updateRule_whenRuleNotFound_thenThrowsNotFound() {
        given(loyaltyRuleRepository.findById(RULE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> businessLoyaltyRuleService.updateRule(
                OWNER_ID,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand(
                        "New title",
                        null,
                        null,
                        null
                )
        )).isInstanceOf(LoyaltyRuleNotFoundException.class);
    }

    @Test
    void updateRule_whenOwnerHasNoAccess_thenThrowsAccessDenied() {
        final LoyaltyRule rule = loyaltyRule(
                "Discount 15%",
                15,
                100,
                false
        );
        given(loyaltyRuleRepository.findById(RULE_ID)).willReturn(Optional.of(rule));
        willThrow(new VenueAccessDeniedException(VENUE_ID)).given(venueOwnershipPort).requireOwner(VENUE_ID, OWNER_ID);

        assertThatThrownBy(() -> businessLoyaltyRuleService.updateRule(
                OWNER_ID,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand(
                        "New title",
                        null,
                        null,
                        null
                )
        )).isInstanceOf(VenueAccessDeniedException.class);
    }

    @Test
    void updateRule_whenRuleIsInactive_thenAllowsEditing() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, false);
        given(loyaltyRuleRepository.findById(RULE_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(4L);

        final LoyaltyRuleDetails result = businessLoyaltyRuleService.updateRule(
                OWNER_ID,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand(
                        "New title",
                        null,
                        null,
                        null
                )
        );

        assertThat(result.rule().getDescription()).isEqualTo("New title");
        assertThat(result.rule().isActive()).isFalse();
        assertThat(result.currentUsages()).isEqualTo(4L);
    }

    @Test
    void updateRule_whenMaxUsagesBelowCurrentUsages_thenThrowsConflict() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, false);
        given(loyaltyRuleRepository.findById(RULE_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(6L);

        assertThatThrownBy(() -> businessLoyaltyRuleService.updateRule(
                OWNER_ID,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand(null, null, 5, null)
        )).isInstanceOf(LoyaltyRuleStateException.class)
                .hasMessage("maxUsages cannot be less than current usages");
    }

    @Test
    void updateRule_whenRuleIsActive_thenThrowsConflict() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, true);
        given(loyaltyRuleRepository.findById(RULE_ID)).willReturn(Optional.of(rule));

        assertThatThrownBy(() -> businessLoyaltyRuleService.updateRule(
                OWNER_ID,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand(
                        "Discount 20%",
                        20,
                        120,
                        false
                )
        )).isInstanceOf(LoyaltyRuleStateException.class)
                .hasMessage("Active loyalty rule cannot be updated");
    }

    @Test
    void updateRule_whenRequestIsValid_thenUpdatesFields() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, false);
        given(loyaltyRuleRepository.findById(RULE_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(4L);

        final LoyaltyRuleDetails result = businessLoyaltyRuleService.updateRule(
                OWNER_ID,
                RULE_ID,
                new BusinessLoyaltyRuleUpdateCommand(
                        "Discount 20%",
                        20,
                        120,
                        false
                )
        );

        assertThat(result.rule().getDescription()).isEqualTo("Discount 20%");
        assertThat(result.rule().getDiscountPercent()).isEqualTo(20);
        assertThat(result.rule().getMaxUsages()).isEqualTo(120);
        assertThat(result.currentUsages()).isEqualTo(4);
        assertThat(result.rule().isActive()).isFalse();
    }

    @Test
    void redeemLoyaltyRule_whenUserAlreadyUsedRule_thenReturnsAlreadyUsedStatus() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, true);
        given(loyaltyQrService.resolveActiveSessionForUpdate(QR_PAYLOAD)).willReturn(qrSession());
        given(loyaltyRuleRepository.findByIdForUpdate(RULE_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.existsByRuleIdAndUserId(RULE_ID, USER_ID)).willReturn(true);

        final LoyaltyActivationResult result = businessLoyaltyRuleService
                .redeemLoyaltyRule(REDEEM_COMMAND);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.status()).isEqualTo(LoyaltyActivationStatus.ALREADY_USED);
        assertThat(result.verification()).isNull();
    }

    @Test
    void redeemLoyaltyRule_whenMaxUsagesReached_thenReturnsLimitExceededStatus() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 2, true);
        given(loyaltyQrService.resolveActiveSessionForUpdate(QR_PAYLOAD)).willReturn(qrSession());
        given(loyaltyRuleRepository.findByIdForUpdate(RULE_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.existsByRuleIdAndUserId(RULE_ID, USER_ID)).willReturn(false);
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(2L);

        final LoyaltyActivationResult result = businessLoyaltyRuleService
                .redeemLoyaltyRule(REDEEM_COMMAND);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.status()).isEqualTo(LoyaltyActivationStatus.LIMIT_EXCEEDED);
        assertThat(result.verification()).isNull();
    }

    @Test
    void redeemLoyaltyRule_whenChecksPass_thenCreatesVerificationAndReturnsSuccessStatus() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, true);
        final LoyaltyVerification savedVerification = new LoyaltyVerification(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                VENUE_ID,
                USER_ID,
                rule,
                15
        );
        savedVerification.setVerifiedAt(OffsetDateTime.parse("2026-04-27T09:00:00+03:00"));

        given(loyaltyQrService.resolveActiveSessionForUpdate(QR_PAYLOAD)).willReturn(qrSession());
        given(loyaltyRuleRepository.findByIdForUpdate(RULE_ID)).willReturn(Optional.of(rule));
        given(loyaltyVerificationRepository.existsByRuleIdAndUserId(RULE_ID, USER_ID)).willReturn(false);
        given(loyaltyVerificationRepository.countByRuleId(RULE_ID)).willReturn(1L);
        given(loyaltyVerificationRepository.save(any(LoyaltyVerification.class))).willReturn(savedVerification);

        final LoyaltyActivationResult result = businessLoyaltyRuleService
                .redeemLoyaltyRule(REDEEM_COMMAND);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.status()).isEqualTo(LoyaltyActivationStatus.SUCCESS);
        assertThat(result.verification()).isEqualTo(savedVerification);
    }

    @Test
    void redeemLoyaltyRule_whenQrBelongsToAnotherVenue_thenThrowsValidationError() {
        final LoyaltyRule rule = loyaltyRule("Discount 15%", 15, 100, true);
        final LoyaltyQrSession wrongVenueSession = new LoyaltyQrSession(
                UUID.randomUUID(),
                "hash",
                USER_ID,
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                RULE_ID,
                OffsetDateTime.parse("2026-05-14T10:05:00Z")
        );
        given(loyaltyQrService.resolveActiveSessionForUpdate(QR_PAYLOAD)).willReturn(wrongVenueSession);
        given(loyaltyRuleRepository.findByIdForUpdate(RULE_ID)).willReturn(Optional.of(rule));

        assertThatThrownBy(() -> businessLoyaltyRuleService.redeemLoyaltyRule(REDEEM_COMMAND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("QR does not belong to requested venue");
    }

    private LoyaltyRule loyaltyRule(
            final String description,
            final int discountPercent,
            final int maxUsages,
            final boolean active
    ) {
        final LoyaltyRule rule = new LoyaltyRule(RULE_ID, VENUE_ID, description, discountPercent, maxUsages);
        rule.setActive(active);
        rule.setCreatedAt(OffsetDateTime.parse("2026-04-27T08:30:00+03:00"));
        return rule;
    }

    private LoyaltyRuleUsageCount usageCount(final UUID ruleId, final long usages) {
        return new LoyaltyRuleUsageCount(ruleId, usages);
    }

    private LoyaltyQrSession qrSession() {
        return new LoyaltyQrSession(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "hash",
                USER_ID,
                VENUE_ID,
                RULE_ID,
                OffsetDateTime.parse("2026-05-14T10:05:00Z")
        );
    }
}
