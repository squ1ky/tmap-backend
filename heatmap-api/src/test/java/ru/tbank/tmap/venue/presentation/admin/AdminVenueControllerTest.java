package ru.tbank.tmap.venue.presentation.admin;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.AdminModerationDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.test.security.TestSecurityConfig;
import ru.tbank.tmap.venue.application.service.admin.AdminVenueService;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.VenueTestFactory;

@WebMvcTest(AdminVenueController.class)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        AdminVenueMapper.class,
})
class AdminVenueControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminVenueService adminVenueService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAdminVenues_whenUserIsAdmin_thenReturnPendingVenues() throws Exception {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.PENDING, null);
        given(adminVenueService.getVenues(VenueStatus.PENDING, 0, 20))
                .willReturn(new PageImpl<>(
                        List.of(new VenueDetails(venue, null)),
                        PageRequest.of(0, 20),
                        1
                ));

        mockMvc.perform(get("/api/v1/admin/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$.items[0].moderationStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAdminVenues_whenUserIsNotAdmin_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/venues"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void verifyAdminVenue_whenUserIsAdmin_thenReturnActivatedVenue() throws Exception {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        given(adminVenueService.verifyAdminVenue(VENUE_ID))
                .willReturn(new VenueDetails(venue, null));

        mockMvc.perform(patch("/api/v1/admin/venues/{id}/verify", VENUE_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectAdminVenue_whenUserIsAdmin_thenReturnRejectedVenue() throws Exception {
        final String rejectReason = "Address does not match coordinates";
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        final VenuePendingUpdate pendingUpdate =
                VenueTestFactory.createPendingUpdate(
                        venue,
                        VenueStatus.REJECTED,
                        rejectReason,
                        VenueTestFactory.defaultUpdatedContent()
                );
        final AdminModerationDecision decision = new AdminModerationDecision()
                .reason(rejectReason);
        given(adminVenueService.rejectAdminVenue(VENUE_ID, rejectReason))
                .willReturn(new VenueDetails(
                        venue,
                        pendingUpdate
                ));

        mockMvc.perform(patch("/api/v1/admin/venues/{id}/reject", VENUE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value(rejectReason));
    }
}
