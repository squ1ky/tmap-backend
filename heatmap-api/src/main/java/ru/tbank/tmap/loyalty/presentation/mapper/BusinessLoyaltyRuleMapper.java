package ru.tbank.tmap.loyalty.presentation.mapper;

import java.util.List;
import java.util.UUID;
import org.openapitools.model.LoyaltyQrResponse;
import org.openapitools.model.LoyaltyRuleCreateRequest;
import org.openapitools.model.LoyaltyRuleResponse;
import org.openapitools.model.LoyaltyRuleUpdateRequest;
import org.openapitools.model.LoyaltyVerifyResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleCreateCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleUpdateCommand;
import ru.tbank.tmap.loyalty.application.query.LoyaltyActivationResult;
import ru.tbank.tmap.loyalty.domain.LoyaltyActivationStatus;
import ru.tbank.tmap.loyalty.application.query.LoyaltyQrView;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;

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

    public LoyaltyRuleResponse toResponse(final LoyaltyRuleDetails details) {
        final LoyaltyRule rule = details.rule();
        final long remainingUsages = Math.max(0L, rule.getMaxUsages() - details.currentUsages());
        return new LoyaltyRuleResponse()
                .id(rule.getId())
                .venueId(rule.getVenueId())
                .description(rule.getDescription())
                .discountPercent(rule.getDiscountPercent())
                .maxUsages(rule.getMaxUsages())
                .remainingUsages(remainingUsages)
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt());
    }

    public List<LoyaltyRuleResponse> toResponseList(final List<LoyaltyRuleDetails> details) {
        return details.stream()
                .map(this::toResponse)
                .toList();
    }

    public LoyaltyVerifyResponse toVerifyResponse(
            final UUID ruleId,
            final LoyaltyActivationResult activationResult
    ) {
        final LoyaltyVerifyResponse response = new LoyaltyVerifyResponse()
                .ruleId(ruleId)
                .status(toApiStatus(activationResult.status()));
        if (activationResult.verification() != null) {
            final LoyaltyVerification verification = activationResult.verification();
            response
                    .id(verification.getId())
                    .venueId(verification.getVenueId())
                    .userId(verification.getUserId())
                    .discountApplied(verification.getDiscountApplied())
                    .verifiedAt(verification.getVerifiedAt());
        }
        return response;
    }

    private org.openapitools.model.LoyaltyActivationStatus toApiStatus(final LoyaltyActivationStatus status) {
        return switch (status) {
            case SUCCESS -> org.openapitools.model.LoyaltyActivationStatus.SUCCESS;
            case ALREADY_USED -> org.openapitools.model.LoyaltyActivationStatus.ALREADY_USED;
            case LIMIT_EXCEEDED -> org.openapitools.model.LoyaltyActivationStatus.LIMIT_EXCEEDED;
        };
    }

    public LoyaltyQrResponse toQrResponse(final LoyaltyQrView qrView) {
        return new LoyaltyQrResponse()
                .venueId(qrView.venueId())
                .ruleId(qrView.ruleId())
                .qrPayload(qrView.qrPayload())
                .expiresAt(qrView.expiresAt());
    }
}
