package ru.tbank.tmap.loyalty.business;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.LoyaltyRuleCreateRequest;
import org.openapitools.model.LoyaltyRuleUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import ru.tbank.tmap.auth.userdetails.CustomUserDetails;
import ru.tbank.tmap.auth.jwt.JwtService;
import ru.tbank.tmap.infrastructure.security.SecurityConfig;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRole;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;

@WebMvcTest(BusinessLoyaltyRuleController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        BusinessLoyaltyRuleControllerTest.TestBeans.class
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

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getBusinessVenueLoyaltyRules_whenOwnerAuthenticated_thenReturnsRules() throws Exception {
        given(businessLoyaltyRuleService.getVenueRules("owner@example.com", VENUE_ID))
                .willReturn(List.of(ruleView(true, 4)));

        mockMvc.perform(get("/api/v1/business/venues/{id}/loyalty-rules", VENUE_ID)
                        .with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$[0].venueId").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$[0].currentUsages").value(4))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getBusinessLoyaltyRuleById_whenOwnerAuthenticated_thenReturnsRule() throws Exception {
        given(businessLoyaltyRuleService.getRuleById("owner@example.com", RULE_ID))
                .willReturn(java.util.Optional.of(ruleView(true, 9)));

        mockMvc.perform(get("/api/v1/business/loyalty-rules/{id}", RULE_ID)
                        .with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$.currentUsages").value(9));
    }

    @Test
    void createBusinessVenueLoyaltyRule_whenRequestValid_thenReturnsCreated() throws Exception {
        given(businessLoyaltyRuleService.createRule(
                org.mockito.ArgumentMatchers.eq("owner@example.com"),
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
                org.mockito.ArgumentMatchers.eq("owner@example.com"),
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

    private BusinessLoyaltyRuleDetails ruleView(final boolean active, final int currentUsages) {
        final User owner = new User(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "owner@example.com",
                "password-hash",
                "owner",
                UserRole.BUSINESS_OWNER
        );
        final Venue venue = new Venue(
                VENUE_ID,
                owner,
                "Venue",
                "Address",
                GeoPoint.of(55.75, 37.62),
                123L,
                VenueCategory.FOOD
        );
        final LoyaltyRule rule = new LoyaltyRule(
                RULE_ID,
                venue,
                "Discount 15%",
                15,
                100
        );
        rule.setActive(active);
        rule.setCreatedAt(OffsetDateTime.parse("2026-04-27T08:30:00+03:00"));
        return new BusinessLoyaltyRuleDetails(rule, currentUsages);
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        BusinessLoyaltyRuleMapper businessLoyaltyRuleMapper() {
            return new BusinessLoyaltyRuleMapper();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            return request -> new CorsConfiguration().applyPermitDefaultValues();
        }
    }
}
