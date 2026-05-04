package ru.tbank.tmap.loyalty.presentation;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.BusinessOwnerLoyaltyApi;
import org.openapitools.model.LoyaltyActivationRequest;
import org.openapitools.model.LoyaltyRuleCreateRequest;
import org.openapitools.model.LoyaltyRuleResponse;
import org.openapitools.model.LoyaltyRuleUpdateRequest;
import org.openapitools.model.LoyaltyVerifyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.loyalty.application.BusinessLoyaltyRuleService;
import ru.tbank.tmap.loyalty.application.command.RedeemLoyaltyRuleCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleCreateCommand;
import ru.tbank.tmap.loyalty.application.command.BusinessLoyaltyRuleUpdateCommand;
import ru.tbank.tmap.loyalty.presentation.dto.BusinessLoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.presentation.mapper.BusinessLoyaltyRuleMapper;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleUpdateValidationException;
import ru.tbank.tmap.shared.utils.SecurityUtils;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BusinessLoyaltyRuleController implements BusinessOwnerLoyaltyApi {

    private final BusinessLoyaltyRuleService businessLoyaltyRuleService;
    private final BusinessLoyaltyRuleMapper businessLoyaltyRuleMapper;

    @Override
    public ResponseEntity<List<LoyaltyRuleResponse>> getBusinessVenueLoyaltyRules(final UUID id) {
        final UUID ownerId = SecurityUtils.currentUserId();
        final List<BusinessLoyaltyRuleDetails> loyaltyRules = businessLoyaltyRuleService.getVenueRules(ownerId, id);
        return ResponseEntity.ok(loyaltyRules.stream()
                .map(businessLoyaltyRuleMapper::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<LoyaltyRuleResponse> createBusinessVenueLoyaltyRule(
            final UUID id,
            @Valid final LoyaltyRuleCreateRequest loyaltyRuleCreateRequest
    ) {
        final UUID ownerId = SecurityUtils.currentUserId();
        final BusinessLoyaltyRuleCreateCommand command =
                businessLoyaltyRuleMapper.toCreateCommand(loyaltyRuleCreateRequest);
        final BusinessLoyaltyRuleDetails loyaltyRule = businessLoyaltyRuleService.createRule(ownerId, id, command);
        final LoyaltyRuleResponse response = businessLoyaltyRuleMapper.toResponse(loyaltyRule);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<LoyaltyRuleResponse> updateBusinessLoyaltyRule(
            final UUID id,
            @Valid final LoyaltyRuleUpdateRequest loyaltyRuleUpdateRequest
    ) {
        final UUID ownerId = SecurityUtils.currentUserId();
        final BusinessLoyaltyRuleUpdateCommand command =
                businessLoyaltyRuleMapper.toUpdateCommand(loyaltyRuleUpdateRequest);
        if (!command.hasChanges()) {
            throw LoyaltyRuleUpdateValidationException.noFieldsForUpdate();
        }
        final BusinessLoyaltyRuleDetails loyaltyRule = businessLoyaltyRuleService.updateRule(ownerId, id, command);
        final LoyaltyRuleResponse response = businessLoyaltyRuleMapper.toResponse(loyaltyRule);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<LoyaltyRuleResponse> getBusinessLoyaltyRuleById(final UUID id) {
        final UUID ownerId = SecurityUtils.currentUserId();
        final BusinessLoyaltyRuleDetails loyaltyRule = businessLoyaltyRuleService.getRuleById(ownerId, id)
                .orElseThrow(() -> new LoyaltyRuleNotFoundException(id));
        final LoyaltyRuleResponse response = businessLoyaltyRuleMapper.toResponse(loyaltyRule);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<LoyaltyVerifyResponse> activateLoyaltyQr(
            final UUID ruleId,
            @Valid final LoyaltyActivationRequest loyaltyActivationRequest
    ) {
        final UUID ownerId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(businessLoyaltyRuleMapper.toVerifyResponse(
                ruleId,
                loyaltyActivationRequest,
                businessLoyaltyRuleService.redeemLoyaltyRule(new RedeemLoyaltyRuleCommand(
                        ownerId,
                        ruleId,
                        loyaltyActivationRequest.getUserId(),
                        loyaltyActivationRequest.getVenueId()
                ))
        ));
    }
}
