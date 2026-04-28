package ru.tbank.tmap.loyalty.business;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openapitools.model.LoyaltyRuleCreateRequest;
import org.openapitools.model.LoyaltyRuleResponse;
import org.openapitools.model.LoyaltyRuleUpdateRequest;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.venue.domain.Venue;

@Component
public class BusinessLoyaltyRuleMapper {

    public BusinessLoyaltyRuleCreateCommand toCreateCommand(final LoyaltyRuleCreateRequest request) {
        return new BusinessLoyaltyRuleCreateCommand(
                request.getDescription(),
                request.getDiscountPercent(),
                request.getMaxUsages()
        );
    }

    public BusinessLoyaltyRuleUpdateCommand toUpdateCommand(final LoyaltyRuleUpdateRequest request) {
        return new BusinessLoyaltyRuleUpdateCommand(
                request.getDescription(),
                request.getDiscountPercent(),
                request.getMaxUsages(),
                request.getActive()
        );
    }

    public LoyaltyRule toEntity(final Venue venue, final BusinessLoyaltyRuleCreateCommand command) {
        return new LoyaltyRule(
                UUID.randomUUID(),
                venue,
                command.description(),
                command.discountPercent(),
                command.maxUsages()
        );
    }

    public LoyaltyRuleResponse toResponse(final BusinessLoyaltyRuleDetails details) {
        final LoyaltyRule rule = details.rule();
        return new LoyaltyRuleResponse()
                .id(rule.getId())
                .venueId(rule.getVenue().getId())
                .description(rule.getDescription())
                .discountPercent(rule.getDiscountPercent())
                .maxUsages(rule.getMaxUsages())
                .currentUsages(details.currentUsages())
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt());
    }

    public List<BusinessLoyaltyRuleDetails> toDetails(
            final List<LoyaltyRule> rules,
            final Map<UUID, Long> usagesByRuleId
    ) {
        return rules.stream()
                .map(rule -> new BusinessLoyaltyRuleDetails(rule, usagesByRuleId.getOrDefault(rule.getId(), 0L)))
                .toList();
    }
}
