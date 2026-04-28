package ru.tbank.tmap.loyalty.business;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.exception.LoyaltyRuleStateException;
import ru.tbank.tmap.loyalty.repository.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.repository.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.repository.LoyaltyVerificationRepository;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserNotFoundException;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessLoyaltyRuleService {

    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final LoyaltyVerificationRepository loyaltyVerificationRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final BusinessLoyaltyRuleMapper businessLoyaltyRuleMapper;

    @Transactional
    public BusinessLoyaltyRuleDetails createRule(
            final String ownerEmail,
            final UUID venueId,
            final BusinessLoyaltyRuleCreateCommand command
    ) {
        final User owner = findOwner(ownerEmail);
        final Venue venue = findOwnedVenue(owner, venueId);
        final LoyaltyRule loyaltyRule = businessLoyaltyRuleMapper.toEntity(venue, command);
        final LoyaltyRule savedRule = loyaltyRuleRepository.save(loyaltyRule);
        return new BusinessLoyaltyRuleDetails(savedRule, 0);
    }

    public List<BusinessLoyaltyRuleDetails> getVenueRules(final String ownerEmail, final UUID venueId) {
        final User owner = findOwner(ownerEmail);
        findOwnedVenue(owner, venueId);
        final List<LoyaltyRule> rules = loyaltyRuleRepository.findByVenueIdAndVenueOwnerIdOrderByCreatedAtDescIdDesc(
                venueId,
                owner.getId()
        );
        return businessLoyaltyRuleMapper.toDetails(rules, getUsagesMap(rules));
    }

    public Optional<BusinessLoyaltyRuleDetails> getRuleById(final String ownerEmail, final UUID ruleId) {
        final User owner = findOwner(ownerEmail);
        return loyaltyRuleRepository.findByIdAndVenueOwnerId(ruleId, owner.getId())
                .map(rule -> new BusinessLoyaltyRuleDetails(
                        rule,
                        loyaltyVerificationRepository.countByRuleId(rule.getId())
                ));
    }

    @Transactional
    public BusinessLoyaltyRuleDetails updateRule(
            final String ownerEmail,
            final UUID ruleId,
            @Valid final BusinessLoyaltyRuleUpdateCommand command
    ) {
        final User owner = findOwner(ownerEmail);
        final LoyaltyRule rule = loyaltyRuleRepository.findByIdAndVenueOwnerId(ruleId, owner.getId())
                .orElseThrow(() -> new LoyaltyRuleNotFoundException(ruleId));

        if (rule.isActive()) {
            throw LoyaltyRuleStateException.activeRuleCannotBeUpdated(ruleId);
        }

        final long currentUsages = loyaltyVerificationRepository.countByRuleId(ruleId);
        applyUpdate(rule, command, currentUsages);
        return new BusinessLoyaltyRuleDetails(rule, currentUsages);
    }

    private Map<UUID, Long> getUsagesMap(final List<LoyaltyRule> rules) {
        if (rules.isEmpty()) {
            return Map.of();
        }
        final List<UUID> ruleIds = rules.stream()
                .map(LoyaltyRule::getId)
                .toList();
        return loyaltyVerificationRepository.countUsagesByRuleIds(ruleIds).stream()
                .collect(java.util.stream.Collectors.toMap(
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
        if (command.maxUsages() != null) {
            if (command.maxUsages() < currentUsages) {
                throw LoyaltyRuleStateException.maxUsagesCannotBeLessThanCurrentUsages(rule.getId());
            }
            rule.setMaxUsages(command.maxUsages());
        }
        if (Boolean.FALSE.equals(command.active())) {
            rule.setActive(false);
        }
    }

    private Venue findOwnedVenue(final User owner, final UUID venueId) {
        return venueRepository.findByIdAndOwnerId(venueId, owner.getId())
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private User findOwner(final String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException(ownerEmail));
    }
}
