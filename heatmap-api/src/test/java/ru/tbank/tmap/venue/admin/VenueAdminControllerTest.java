package ru.tbank.tmap.venue.admin;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import ru.tbank.tmap.infrastructure.security.SecurityConfig;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRole;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueStatus;

@WebMvcTest(VenueAdminController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        VenueModerationMapper.class,
        VenueAdminControllerTest.TestBeans.class
})
class VenueAdminControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VenueModerationService venueModerationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAdminVenues_whenUserIsAdmin_thenReturnPendingVenues() throws Exception {
        given(venueModerationService.getAdminVenues(VenueStatus.PENDING, 0, 20))
                .willReturn(new PageImpl<>(List.of(venue(VenueStatus.PENDING, null)), PageRequest.of(0, 20), 1));

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
        given(venueModerationService.verifyAdminVenue(VENUE_ID))
                .willReturn(venue(VenueStatus.ACTIVE, null));

        mockMvc.perform(patch("/api/v1/admin/venues/{id}/verify", VENUE_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectAdminVenue_whenUserIsAdmin_thenReturnRejectedVenue() throws Exception {
        final AdminModerationDecision decision = new AdminModerationDecision()
                .reason("Address does not match coordinates");
        given(venueModerationService.rejectAdminVenue(VENUE_ID, "Address does not match coordinates"))
                .willReturn(venue(VenueStatus.REJECTED, "Address does not match coordinates"));

        mockMvc.perform(patch("/api/v1/admin/venues/{id}/reject", VENUE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("Address does not match coordinates"));
    }

    private Venue venue(
            final VenueStatus status,
            final String rejectReason
    ) {
        final User owner = new User(
                OWNER_ID,
                "owner@example.com",
                "password-hash",
                "Owner",
                UserRole.BUSINESS_OWNER
        );
        final Venue venue = new Venue(
                VENUE_ID,
                owner,
                "Bar One",
                "Kazan Center, 2",
                GeoPoint.of(55.7905, 49.1140),
                617422037122678783L,
                VenueCategory.ENTERTAINMENT
        );
        venue.setStatus(status);
        venue.setRejectReason(rejectReason);
        return venue;
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            return request -> new CorsConfiguration().applyPermitDefaultValues();
        }
    }
}
