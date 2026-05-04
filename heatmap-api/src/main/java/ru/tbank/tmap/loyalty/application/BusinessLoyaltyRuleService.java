package ru.tbank.tmap.loyalty.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.LoyaltyActivationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleCreateCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleUpdateCommand;
import ru.tbank.tmap.loyalty.application.port.VenueOwnershipPort;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;
import ru.tbank.tmap.loyalty.presentation.dto.BusinessLoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.presentation.mapper.BusinessLoyaltyRuleMapper;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleStateException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessLoyaltyRuleService {

    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final LoyaltyVerificationRepository loyaltyVerificationRepository;
    private final VenueOwnershipPort venueOwnershipPort;
    private final BusinessLoyaltyRuleMapper businessLoyaltyRuleMapper;

    @Transactional
    public BusinessLoyaltyRuleDetails createRule(
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

        return new BusinessLoyaltyRuleDetails(savedRule, currentUsages);
    }

    public List<BusinessLoyaltyRuleDetails> getVenueRules(final UUID ownerId, final UUID venueId) {
        venueOwnershipPort.requireOwner(venueId, ownerId);

        final List<LoyaltyRule> rules = loyaltyRuleRepository.findByVenueIdOrderByCreatedAtDescIdDesc(venueId);

        return businessLoyaltyRuleMapper.toDetails(rules, getUsagesMap(rules));
    }

    public Optional<BusinessLoyaltyRuleDetails> getRuleById(final UUID ownerId, final UUID ruleId) {
        return loyaltyRuleRepository.findById(ruleId)
                .map(rule -> {
                    venueOwnershipPort.requireOwner(rule.getVenueId(), ownerId);

                    return new BusinessLoyaltyRuleDetails(
                            rule,
                            loyaltyVerificationRepository.countByRuleId(rule.getId())
                    );
                });
    }

    @Transactional
    public BusinessLoyaltyRuleDetails updateRule(
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

        return new BusinessLoyaltyRuleDetails(rule, currentUsages);
    }

    @Transactional
    public LoyaltyActivationResult activateRule(final UUID ownerId, final UUID ruleId, final UUID userId) {
        final LoyaltyRule rule = loyaltyRuleRepository.findById(ruleId)
                .orElseThrow(() -> new LoyaltyRuleNotFoundException(ruleId));

        venueOwnershipPort.requireOwner(rule.getVenueId(), ownerId);

        if (!rule.isActive()) {
            throw LoyaltyRuleStateException.inactiveRuleCannotBeActivated(ruleId);
        }

        if (loyaltyVerificationRepository.existsByRuleIdAndUserId(ruleId, userId)) {
            return new LoyaltyActivationResult(LoyaltyActivationStatus.ALREADY_USED, null);
        }

        final long currentUsages = loyaltyVerificationRepository.countByRuleId(ruleId);
        if (currentUsages >= rule.getMaxUsages()) {
            return new LoyaltyActivationResult(LoyaltyActivationStatus.LIMIT_EXCEEDED, null);
        }

        final LoyaltyVerification loyaltyVerification = loyaltyVerificationRepository.save(
                new LoyaltyVerification(
                        UUID.randomUUID(),
                        rule.getVenueId(),
                        userId,
                        rule,
                        rule.getDiscountPercent()
                )
        );
        return new LoyaltyActivationResult(LoyaltyActivationStatus.SUCCESS, loyaltyVerification);
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

    public record LoyaltyActivationResult(
            LoyaltyActivationStatus status,
            LoyaltyVerification verification
    ) {
    }
}
