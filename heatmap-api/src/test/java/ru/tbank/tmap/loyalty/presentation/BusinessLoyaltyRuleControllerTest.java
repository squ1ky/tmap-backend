package ru.tbank.tmap.loyalty.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.LoyaltyActivationRequest;
import org.openapitools.model.LoyaltyRuleCreateRequest;
import org.openapitools.model.LoyaltyRuleUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;
import ru.tbank.tmap.loyalty.application.BusinessLoyaltyRuleService;
import ru.tbank.tmap.loyalty.application.query.BusinessLoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.application.query.LoyaltyActivationResult;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.domain.LoyaltyActivationStatus;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;
import ru.tbank.tmap.loyalty.presentation.mapper.BusinessLoyaltyRuleMapper;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.infrastructure.security.TestSecurityConfig;

@WebMvcTest(BusinessLoyaltyRuleController.class)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        BusinessLoyaltyRuleMapper.class,
})
class BusinessLoyaltyRuleControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessLoyaltyRuleService businessLoyaltyRuleService;

    @Test
    void getBusinessVenueLoyaltyRules_whenOwnerAuthenticated_thenReturnsRules() throws Exception {
        given(businessLoyaltyRuleService.getVenueRules(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                VENUE_ID
        ))
                .willReturn(List.of(ruleView(true, 4)));

        mockMvc.perform(get("/api/v1/business/venues/{id}/loyalty-rules", VENUE_ID)
                        .with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$[0].venueId").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$[0].remainingUsages").value(96))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBusinessLoyaltyRuleById_whenOwnerAuthenticated_thenReturnsRule() throws Exception {
        given(businessLoyaltyRuleService.getRuleById(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                RULE_ID
        ))
                .willReturn(java.util.Optional.of(ruleView(true, 9)));

        mockMvc.perform(get("/api/v1/business/loyalty-rules/{id}", RULE_ID)
                        .with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$.remainingUsages").value(91));
    }

    @Test
    void createBusinessVenueLoyaltyRule_whenRequestValid_thenReturnsCreated() throws Exception {
        given(businessLoyaltyRuleService.createRule(
                org.mockito.ArgumentMatchers.eq(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                org.mockito.ArgumentMatchers.eq(VENUE_ID),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(ruleView(true, 0));

        final LoyaltyRuleCreateRequest request = new LoyaltyRuleCreateRequest("Discount 15%", 15, 100);

        mockMvc.perform(post("/api/v1/business/venues/{id}/loyalty-rules", VENUE_ID)
                        .with(user(ownerPrincipal()))
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Discount 15%"))
                .andExpect(jsonPath("$.maxUsages").value(100));
    }

    @Test
    void createBusinessVenueLoyaltyRule_whenRequestInvalid_thenReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/business/venues/{id}/loyalty-rules", VENUE_ID)
                        .with(user(ownerPrincipal()))
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "",
                                "discountPercent", 101,
                                "maxUsages", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateBusinessLoyaltyRule_whenRequestValid_thenReturnsUpdatedRule() throws Exception {
        given(businessLoyaltyRuleService.updateRule(
                org.mockito.ArgumentMatchers.eq(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                org.mockito.ArgumentMatchers.eq(RULE_ID),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(ruleView(false, 5));

        final LoyaltyRuleUpdateRequest request = new LoyaltyRuleUpdateRequest()
                .description("Discount 15%")
                .active(false);

        mockMvc.perform(patch("/api/v1/business/loyalty-rules/{id}", RULE_ID)
                        .with(user(ownerPrincipal()))
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateBusinessLoyaltyRule_whenRequestInvalid_thenReturnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/business/loyalty-rules/{id}", RULE_ID)
                        .with(user(ownerPrincipal()))
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "",
                                "discountPercent", -1,
                                "maxUsages", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void activateLoyaltyQr_whenRequestValid_thenReturnsActivationResult() throws Exception {
        given(businessLoyaltyRuleService.redeemLoyaltyRule(org.mockito.ArgumentMatchers.any()))
                .willReturn(new LoyaltyActivationResult(
                        RULE_ID,
                        LoyaltyActivationStatus.SUCCESS,
                        verification()
                ));

        final LoyaltyActivationRequest request = new LoyaltyActivationRequest()
                .qrPayload("lqr:1:test-token");

        mockMvc.perform(post("/api/v1/business/loyalty-rules/activate")
                        .with(user(ownerPrincipal()))
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value(RULE_ID.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.venueId").value(VENUE_ID.toString()));
    }

    @Test
    void getBusinessLoyaltyRuleHistory_whenOwnerAuthenticated_thenReturnsHistoryPage() throws Exception {
        given(businessLoyaltyRuleService.getRuleHistory(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                RULE_ID,
                0,
                20
        )).willReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
        ));

        mockMvc.perform(get("/api/v1/business/loyalty-rules/{id}/history", RULE_ID)
                        .with(user(ownerPrincipal()))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBusinessVenueLoyaltyRules_whenUserIsNotOwnerRole_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/business/venues/{id}/loyalty-rules", VENUE_ID))
                .andExpect(status().isForbidden());
    }

    private CustomUserDetails ownerPrincipal() {
        return new CustomUserDetails(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "owner@example.com",
                "password-hash",
                true,
                true,
                true,
                true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_BUSINESS_OWNER"
                ))
        );
    }

    private LoyaltyRuleDetails ruleView(final boolean active, final int currentUsages) {
        final LoyaltyRule rule = new LoyaltyRule(RULE_ID, VENUE_ID, "Discount 15%", 15, 100);
        rule.setActive(active);
        rule.setCreatedAt(OffsetDateTime.parse("2026-04-27T08:30:00+03:00"));
        return new LoyaltyRuleDetails(rule, currentUsages);
    }

    private LoyaltyVerification verification() {
        final LoyaltyRule rule = new LoyaltyRule(RULE_ID, VENUE_ID, "Discount 15%", 15, 100);
        final LoyaltyVerification verification = new LoyaltyVerification(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                VENUE_ID,
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                rule,
                15
        );
        verification.setVerifiedAt(OffsetDateTime.parse("2026-05-24T10:00:00+03:00"));
        return verification;
    }
}
