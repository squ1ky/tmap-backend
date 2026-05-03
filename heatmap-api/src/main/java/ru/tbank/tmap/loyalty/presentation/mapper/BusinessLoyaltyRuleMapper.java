package ru.tbank.tmap.loyalty.presentation.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openapitools.model.LoyaltyRuleCreateRequest;
import org.openapitools.model.LoyaltyRuleResponse;
import org.openapitools.model.LoyaltyRuleUpdateRequest;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleCreateCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleUpdateCommand;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.presentation.dto.BusinessLoyaltyRuleDetails;

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

    public LoyaltyRuleResponse toResponse(final BusinessLoyaltyRuleDetails details) {
        final LoyaltyRule rule = details.rule();
        return new LoyaltyRuleResponse()
                .id(rule.getId())
                .venueId(rule.getVenueId())
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
