package ru.tbank.tmap.loyalty.business;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.exception.LoyaltyRuleStateException;
import ru.tbank.tmap.loyalty.exception.LoyaltyRuleValidationException;
import ru.tbank.tmap.loyalty.repository.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.repository.LoyaltyVerificationRepository;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserNotFoundException;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Service
@Transactional(readOnly = true)
public class BusinessLoyaltyRuleService {

    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final int MIN_DISCOUNT_PERCENT = 0;
    private static final int MAX_DISCOUNT_PERCENT = 100;

    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final LoyaltyVerificationRepository loyaltyVerificationRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final BusinessLoyaltyRuleMapper businessLoyaltyRuleMapper;

    public BusinessLoyaltyRuleService(
            final LoyaltyRuleRepository loyaltyRuleRepository,
            final LoyaltyVerificationRepository loyaltyVerificationRepository,
            final VenueRepository venueRepository,
            final UserRepository userRepository,
            final BusinessLoyaltyRuleMapper businessLoyaltyRuleMapper
    ) {
        this.loyaltyRuleRepository = loyaltyRuleRepository;
        this.loyaltyVerificationRepository = loyaltyVerificationRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.businessLoyaltyRuleMapper = businessLoyaltyRuleMapper;
    }

    @Transactional
    public LoyaltyRuleDetails createRule(
            final String ownerEmail,
            final UUID venueId,
            final BusinessLoyaltyRuleCreateCommand command
    ) {
        validateDescription(command.description());
        validateDiscountPercent(command.discountPercent().doubleValue());
        validateMaxUsages(command.maxUsages());
        final Venue venue = findOwnedVenue(ownerEmail, venueId);
        final LoyaltyRule loyaltyRule = businessLoyaltyRuleMapper.toEntity(venue, command);
        final LoyaltyRule savedRule = loyaltyRuleRepository.save(loyaltyRule);
        return new LoyaltyRuleDetails(savedRule, 0);
    }

    public List<LoyaltyRuleDetails> getVenueRules(final String ownerEmail, final UUID venueId) {
        findOwnedVenue(ownerEmail, venueId);
        final User owner = findOwner(ownerEmail);
        final List<LoyaltyRule> rules = loyaltyRuleRepository.findByVenueIdAndVenueOwnerIdOrderByCreatedAtDescIdDesc(
                venueId,
                owner.getId()
        );
        return toDetails(rules);
    }

    public Optional<LoyaltyRuleDetails> getRuleById(final String ownerEmail, final UUID ruleId) {
        final User owner = findOwner(ownerEmail);
        return loyaltyRuleRepository.findByIdAndVenueOwnerId(ruleId, owner.getId())
                .map(rule -> new LoyaltyRuleDetails(
                        rule,
                        Math.toIntExact(loyaltyVerificationRepository.countByRuleId(rule.getId()))
                ));
    }

    @Transactional
    public LoyaltyRuleDetails updateRule(
            final String ownerEmail,
            final UUID ruleId,
            final BusinessLoyaltyRuleUpdateCommand command
    ) {
        if (!command.hasChanges()) {
            throw LoyaltyRuleValidationException.noFieldsForUpdate();
        }

        final User owner = findOwner(ownerEmail);
        final LoyaltyRule rule = loyaltyRuleRepository.findByIdAndVenueOwnerId(ruleId, owner.getId())
                .orElseThrow(() -> new LoyaltyRuleNotFoundException(ruleId));

        if (!rule.isActive()) {
            throw LoyaltyRuleStateException.inactiveRuleCannotBeUpdated(ruleId);
        }

        final int currentUsages = Math.toIntExact(loyaltyVerificationRepository.countByRuleId(ruleId));
        applyUpdate(rule, command, currentUsages);
        return new LoyaltyRuleDetails(rule, currentUsages);
    }

    private List<LoyaltyRuleDetails> toDetails(final List<LoyaltyRule> rules) {
        if (rules.isEmpty()) {
            return List.of();
        }
        final List<UUID> ruleIds = rules.stream()
                .map(LoyaltyRule::getId)
                .toList();
        final Map<UUID, Integer> usagesByRuleId = loyaltyVerificationRepository.countUsagesByRuleIds(ruleIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        LoyaltyVerificationRepository.LoyaltyRuleUsageCount::getRuleId,
                        usage -> Math.toIntExact(usage.getUsages())
                ));

        return rules.stream()
                .map(rule -> new LoyaltyRuleDetails(rule, usagesByRuleId.getOrDefault(rule.getId(), 0)))
                .toList();
    }

    private void applyUpdate(
            final LoyaltyRule rule,
            final BusinessLoyaltyRuleUpdateCommand command,
            final int currentUsages
    ) {
        if (command.description() != null) {
            validateDescription(command.description());
            rule.setDescription(command.description().trim());
        }
        if (command.discountPercent() != null) {
            validateDiscountPercent(command.discountPercent().doubleValue());
            rule.setDiscountPercent(command.discountPercent());
        }
        if (command.maxUsages() != null) {
            validateMaxUsages(command.maxUsages());
            if (command.maxUsages() < currentUsages) {
                throw LoyaltyRuleStateException.maxUsagesCannotBeLessThanCurrentUsages(rule.getId());
            }
            rule.setMaxUsages(command.maxUsages());
        }
        if (Boolean.FALSE.equals(command.active())) {
            rule.setActive(false);
        }
    }

    private Venue findOwnedVenue(final String ownerEmail, final UUID venueId) {
        final User owner = findOwner(ownerEmail);
        return venueRepository.findByIdAndOwnerId(venueId, owner.getId())
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private User findOwner(final String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException(ownerEmail));
    }

    private void validateDescription(final String description) {
        if (description == null || description.isBlank()) {
            throw LoyaltyRuleValidationException.blankDescription();
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw LoyaltyRuleValidationException.descriptionTooLong(MAX_DESCRIPTION_LENGTH);
        }
    }

    private void validateDiscountPercent(final double discountPercent) {
        if (discountPercent < MIN_DISCOUNT_PERCENT || discountPercent > MAX_DISCOUNT_PERCENT) {
            throw LoyaltyRuleValidationException.invalidDiscountPercent(
                    MIN_DISCOUNT_PERCENT,
                    MAX_DISCOUNT_PERCENT
            );
        }
    }

    private void validateMaxUsages(final int maxUsages) {
        if (maxUsages <= 0) {
            throw LoyaltyRuleValidationException.maxUsagesMustBeGreaterThanZero();
        }
    }
}
