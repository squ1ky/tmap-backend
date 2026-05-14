package ru.tbank.tmap.loyalty.presentation.mapper;

import java.util.UUID;
import org.openapitools.model.LoyaltyQrResponse;
import org.openapitools.model.LoyaltyRuleCreateRequest;
import org.openapitools.model.LoyaltyRuleResponse;
import org.openapitools.model.LoyaltyRuleUpdateRequest;
import org.openapitools.model.LoyaltyVerifyResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleCreateCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleUpdateCommand;
import ru.tbank.tmap.loyalty.application.command.LoyaltyActivationResult;
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
        final long remainingUsages = Math.max(0L, (long) rule.getMaxUsages() - details.currentUsages());
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

    public LoyaltyVerifyResponse toVerifyResponse(
            final UUID ruleId,
            final LoyaltyActivationResult activationResult
    ) {
        final LoyaltyVerifyResponse response = new LoyaltyVerifyResponse()
                .ruleId(ruleId)
                .status(activationResult.status());
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

    public LoyaltyQrResponse toQrResponse(final LoyaltyQrView qrView) {
        return new LoyaltyQrResponse()
                .venueId(qrView.venueId())
                .ruleId(qrView.ruleId())
                .qrPayload(qrView.qrPayload())
                .expiresAt(qrView.expiresAt());
    }
}
