package ru.tbank.tmap.loyalty.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.application.command.RedeemLoyaltyRuleCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleCreateCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleUpdateCommand;
import ru.tbank.tmap.loyalty.application.port.VenueOwnershipPort;
import ru.tbank.tmap.loyalty.application.query.LoyaltyActivationResult;
import ru.tbank.tmap.loyalty.domain.LoyaltyActivationStatus;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSession;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyQrValidationException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleStateException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessLoyaltyRuleService {

    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final LoyaltyVerificationRepository loyaltyVerificationRepository;
    private final VenueOwnershipPort venueOwnershipPort;
    private final LoyaltyQrService loyaltyQrService;

    @Transactional
    public LoyaltyRuleDetails createRule(
            final UUID ownerId,
            final UUID venueId,
            final BusinessLoyaltyRuleCreateCommand command
    ) {
        venueOwnershipPort.requireOwner(venueId, ownerId);

        final int currentUsages = 0;
        final LoyaltyRule loyaltyRule = new LoyaltyRule(
                UUID.randomUUID(),
                venueId,
                command.description(),
                command.discountPercent(),
                command.maxUsages()
        );
        final LoyaltyRule savedRule = loyaltyRuleRepository.save(loyaltyRule);

        return new LoyaltyRuleDetails(savedRule, currentUsages);
    }

    public List<LoyaltyRuleDetails> getVenueRules(final UUID ownerId, final UUID venueId) {
        venueOwnershipPort.requireOwner(venueId, ownerId);

        final List<LoyaltyRule> rules = loyaltyRuleRepository.findByVenueIdOrderByCreatedAtDescIdDesc(venueId);
        final Map<UUID, Long> usagesByRuleId = getUsagesMap(rules);

        return rules.stream()
                .map(rule -> new LoyaltyRuleDetails(rule, usagesByRuleId.getOrDefault(rule.getId(), 0L)))
                .toList();
    }

    public Optional<LoyaltyRuleDetails> getRuleById(final UUID ownerId, final UUID ruleId) {
        return loyaltyRuleRepository.findById(ruleId)
                .map(rule -> {
                    venueOwnershipPort.requireOwner(rule.getVenueId(), ownerId);

                    return new LoyaltyRuleDetails(
                            rule,
                            loyaltyVerificationRepository.countByRuleId(rule.getId())
                    );
                });
    }

    @Transactional
    public LoyaltyRuleDetails updateRule(
            final UUID ownerId,
            final UUID ruleId,
            final BusinessLoyaltyRuleUpdateCommand command
    ) {
        final LoyaltyRule rule = loyaltyRuleRepository.findById(ruleId)
                .orElseThrow(() -> new LoyaltyRuleNotFoundException(ruleId));

        venueOwnershipPort.requireOwner(rule.getVenueId(), ownerId);

        if (rule.isActive()) {
            throw LoyaltyRuleStateException.activeRuleCannotBeUpdated(ruleId);
        }

        final long currentUsages = loyaltyVerificationRepository.countByRuleId(ruleId);
        applyUpdate(rule, command, currentUsages);

        return new LoyaltyRuleDetails(rule, currentUsages);
    }

    @Transactional
    public LoyaltyActivationResult redeemLoyaltyRule(final RedeemLoyaltyRuleCommand command) {
        final LoyaltyRule rule = loyaltyRuleRepository.findByIdForUpdate(command.ruleId())
                .orElseThrow(() -> new LoyaltyRuleNotFoundException(command.ruleId()));

        venueOwnershipPort.requireOwner(rule.getVenueId(), command.ownerId());

        if (!rule.isActive()) {
            throw LoyaltyRuleStateException.inactiveRuleCannotBeRedeemed(command.ruleId());
        }

        final LoyaltyQrSession qrSession = validateQrSession(rule, command);

        if (loyaltyVerificationRepository.existsByRuleIdAndUserId(command.ruleId(), qrSession.getUserId())) {
            loyaltyQrService.markConsumed(qrSession);
            return new LoyaltyActivationResult(LoyaltyActivationStatus.ALREADY_USED, null);
        }

        final long currentUsages = loyaltyVerificationRepository.countByRuleId(command.ruleId());
        if (currentUsages >= rule.getMaxUsages()) {
            loyaltyQrService.markConsumed(qrSession);
            return new LoyaltyActivationResult(LoyaltyActivationStatus.LIMIT_EXCEEDED, null);
        }

        final LoyaltyVerification loyaltyVerification;
        try {
            loyaltyVerification = loyaltyVerificationRepository.save(
                    new LoyaltyVerification(
                            UUID.randomUUID(),
                            rule.getVenueId(),
                            qrSession.getUserId(),
                            rule,
                            rule.getDiscountPercent()
                    )
            );
        } catch (DataIntegrityViolationException ex) {
            loyaltyQrService.markConsumed(qrSession);
            return new LoyaltyActivationResult(LoyaltyActivationStatus.ALREADY_USED, null);
        }
        loyaltyQrService.markConsumed(qrSession);
        return new LoyaltyActivationResult(LoyaltyActivationStatus.SUCCESS, loyaltyVerification);
    }

    private LoyaltyQrSession validateQrSession(
            final LoyaltyRule rule,
            final RedeemLoyaltyRuleCommand command
    ) {
        final LoyaltyQrSession qrSession = loyaltyQrService.resolveActiveSessionForUpdate(command.qrPayload());
        if (!qrSession.getRuleId().equals(command.ruleId())) {
            throw LoyaltyQrValidationException.qrDoesNotBelongToRequestedRule();
        }
        if (!qrSession.getVenueId().equals(rule.getVenueId())) {
            throw LoyaltyQrValidationException.qrDoesNotBelongToRequestedVenue();
        }
        return qrSession;
    }

    private Map<UUID, Long> getUsagesMap(final List<LoyaltyRule> rules) {
        if (rules.isEmpty()) {
            return Map.of();
        }

        final List<UUID> ruleIds = rules.stream()
                .map(LoyaltyRule::getId)
                .toList();

        return loyaltyVerificationRepository.countUsagesByRuleIds(ruleIds).stream()
                .collect(Collectors.toMap(
                        LoyaltyRuleUsageCount::ruleId,
                        LoyaltyRuleUsageCount::usages
                ));
    }

    private void applyUpdate(
            final LoyaltyRule rule,
            final BusinessLoyaltyRuleUpdateCommand command,
            final long currentUsages
    ) {
        if (command.description() != null) {
            rule.setDescription(command.description().trim());
        }
        if (command.discountPercent() != null) {
            rule.setDiscountPercent(command.discountPercent());
        }

        rule.updateMaxUsages(command.maxUsages(), currentUsages);

        if (Boolean.FALSE.equals(command.active())) {
            rule.setActive(false);
        }
    }

}
