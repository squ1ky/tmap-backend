package ru.tbank.tmap.venue.presentation.business;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.openapitools.model.VenueCreateRequest;
import org.openapitools.model.VenueUpdateRequest;
import org.openapitools.model.VenueOwnerResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;
import ru.tbank.tmap.loyalty.api.LoyaltyRuleFacade;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.presentation.mapper.BusinessLoyaltyRuleMapper;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.infrastructure.security.TestSecurityConfig;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.application.command.VenueUpdateCommand;
import ru.tbank.tmap.venue.application.service.business.BusinessVenueService;
import ru.tbank.tmap.venue.application.service.business.photo.BusinessVenuePhotoService;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.VenueTestFactory;

@WebMvcTest(BusinessVenueController.class)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        BusinessVenueMapper.class,
        BusinessLoyaltyRuleMapper.class,
})
class BusinessVenueControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessVenueService businessVenueService;

    @MockitoBean
    private BusinessVenuePhotoService businessVenuePhotoService;

    @MockitoBean
    private BusinessVenueOwnerMapper venueOwnerMapper;

    @MockitoBean
    private LoyaltyRuleFacade loyaltyRuleFacade;

    @Test
    void createVenue_whenUserAuthenticated_thenReturnsCreated() throws Exception {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.PENDING, null);
        given(businessVenueService.createVenue(
                org.mockito.ArgumentMatchers.eq(OWNER_ID),
                org.mockito.ArgumentMatchers.any(ru.tbank.tmap.venue.application.command.VenueCreateCommand.class)
        )).willReturn(new VenueDetails(venue, null));
        given(loyaltyRuleFacade.getActiveVenueRules(VENUE_ID)).willReturn(List.of(ruleView()));
        given(venueOwnerMapper.toResponse(
                org.mockito.ArgumentMatchers.any(VenueDetails.class),
                org.mockito.ArgumentMatchers.anyList()
        ))
                .willReturn(new VenueOwnerResponse()
                        .id(VENUE_ID)
                        .name("Bar One")
                        .moderationStatus(VenueModerationStatus.PENDING));

        final VenueCreateRequest request = new VenueCreateRequest()
                .name("Bar One")
                .address("Kazan Center, 1")
                .description("Fresh coffee")
                .lat(55.7905)
                .lng(49.1140)
                .category(VenueCreateRequest.CategoryEnum.FOOD);

        mockMvc.perform(post("/api/v1/business/venues")
                        .with(user(userPrincipal()))
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bar One"))
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"));
    }

    @Test
    void updateVenue_whenOwnerAuthenticated_thenReturnsPublishedDataWithPendingStatus() throws Exception {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        final VenuePendingUpdate pendingUpdate = VenueTestFactory.createPendingUpdate(venue, VenueStatus.PENDING_UPDATE);
        given(businessVenueService.updateVenue(
                org.mockito.ArgumentMatchers.eq(OWNER_ID),
                org.mockito.ArgumentMatchers.eq(VENUE_ID),
                org.mockito.ArgumentMatchers.any(VenueUpdateCommand.class)
        )).willReturn(new VenueDetails(venue, pendingUpdate));
        given(loyaltyRuleFacade.getActiveVenueRules(VENUE_ID)).willReturn(List.of(ruleView()));
        given(venueOwnerMapper.toResponse(
                org.mockito.ArgumentMatchers.any(VenueDetails.class),
                org.mockito.ArgumentMatchers.anyList()
        ))
                .willReturn(new VenueOwnerResponse()
                        .id(VENUE_ID)
                        .name("Bar One")
                        .moderationStatus(VenueModerationStatus.PENDING_UPDATE));

        final VenueUpdateRequest request = new VenueUpdateRequest()
                .name("Bar Two")
                .address("Kazan Center, 5")
                .description("Updated description")
                .lat(55.8000)
                .lng(49.1300)
                .category(VenueUpdateRequest.CategoryEnum.FOOD)
                .dishOfDay("Soup")
                .music("Jazz");

        mockMvc.perform(put("/api/v1/business/venues/{id}", VENUE_ID)
                        .with(user(ownerPrincipal()))
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bar One"))
                .andExpect(jsonPath("$.moderationStatus").value("PENDING_UPDATE"));
    }

    private LoyaltyRuleDetails ruleView() {
        final ru.tbank.tmap.loyalty.domain.LoyaltyRule rule = new ru.tbank.tmap.loyalty.domain.LoyaltyRule(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                VENUE_ID,
                "Discount 15%",
                15,
                100
        );
        return new LoyaltyRuleDetails(rule, 4L);
    }

    private CustomUserDetails userPrincipal() {
        return new CustomUserDetails(
                OWNER_ID,
                "user@example.com",
                "password-hash",
                true,
                true,
                true,
                true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_USER"
                ))
        );
    }

    private CustomUserDetails ownerPrincipal() {
        return new CustomUserDetails(
                OWNER_ID,
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
}
