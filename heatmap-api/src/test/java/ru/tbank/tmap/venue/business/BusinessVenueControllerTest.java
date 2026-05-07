package ru.tbank.tmap.venue.business;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.openapitools.model.VenueUpdateRequest;
import org.openapitools.model.VenueOwnerResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.test.security.TestSecurityConfig;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.application.command.VenueUpdateCommand;
import ru.tbank.tmap.venue.application.service.business.BusinessVenueService;
import ru.tbank.tmap.venue.application.service.business.photo.BusinessVenuePhotoService;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.presentation.business.BusinessVenueController;
import ru.tbank.tmap.venue.presentation.business.BusinessVenueMapper;
import ru.tbank.tmap.venue.presentation.business.BusinessVenueOwnerMapper;

@WebMvcTest(BusinessVenueController.class)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        BusinessVenueMapper.class,
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

    @Test
    void updateVenue_whenOwnerAuthenticated_thenReturnsPublishedDataWithPendingStatus() throws Exception {
        final Venue venue = venue(VenueStatus.ACTIVE, null);
        final VenuePendingUpdate pendingUpdate = new VenuePendingUpdate(venue);
        pendingUpdate.setName("Bar Two");
        pendingUpdate.setAddress("Kazan Center, 5");
        pendingUpdate.setLocation(GeoPoint.of(55.8000, 49.1300));
        pendingUpdate.setH3Res9(617422037122678784L);
        pendingUpdate.setCategory(VenueCategory.FOOD);
        pendingUpdate.setStatus(VenueStatus.PENDING_UPDATE);
        given(businessVenueService.updateVenue(
                org.mockito.ArgumentMatchers.eq(OWNER_ID),
                org.mockito.ArgumentMatchers.eq(VENUE_ID),
                org.mockito.ArgumentMatchers.any(VenueUpdateCommand.class)
        )).willReturn(new VenueDetails(venue, pendingUpdate));
        given(venueOwnerMapper.toResponse(org.mockito.ArgumentMatchers.any(VenueDetails.class)))
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

    private Venue venue(final VenueStatus status, final String rejectReason) {
        final Venue venue = Venue.builder()
                .id(VENUE_ID)
                .ownerId(OWNER_ID)
                .name("Bar one")
                .address("Kazan Center, 2")
                .location(GeoPoint.of(55.7905, 49.1140))
                .h3Res9(617422037122678783L)
                .category(VenueCategory.ENTERTAINMENT)
                .build();
        venue.setStatus(status);
        venue.setRejectReason(rejectReason);
        return venue;
    }
}
