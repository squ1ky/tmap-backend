package ru.tbank.tmap.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.VenuePublicResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.venue.VenueCategory;
import ru.tbank.tmap.exception.GlobalExceptionHandler;
import ru.tbank.tmap.service.VenueService;

@WebMvcTest(VenueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VenueControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @Test
    void getVenuesInViewport_whenRequestIsValid_thenReturnVenues() throws Exception {
        given(venueService.getVenuesInViewport(
                new BoundingBox(55.7801, 49.1102, 55.7995, 49.1355),
                List.of()))
                .willReturn(List.of(venueResponse()));

        mockMvc.perform(get("/api/v1/venues")
                        .param("swLat", "55.7801")
                        .param("swLng", "49.1102")
                        .param("neLat", "55.7995")
                        .param("neLng", "49.1355"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Bar One"))
                .andExpect(jsonPath("$[0].category").value("entertainment"));
    }

    @Test
    void getVenuesInViewport_whenCategoryIsProvided_thenPassCategoryFilter() throws Exception {
        given(venueService.getVenuesInViewport(
                new BoundingBox(55.7801, 49.1102, 55.7995, 49.1355),
                List.of(VenueCategory.FOOD)))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/venues")
                        .param("swLat", "55.7801")
                        .param("swLng", "49.1102")
                        .param("neLat", "55.7995")
                        .param("neLng", "49.1355")
                        .param("category", "food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getVenuesInViewport_whenBoundsAreInvalid_thenReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/venues")
                        .param("swLat", "55.7995")
                        .param("swLng", "49.1355")
                        .param("neLat", "55.7801")
                        .param("neLng", "49.1102"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid map bounds"));
    }

    @Test
    void getVenueById_whenVenueExists_thenReturnVenue() throws Exception {
        given(venueService.getVenueById(VENUE_ID))
                .willReturn(Optional.of(venueResponse()));

        mockMvc.perform(get("/api/v1/venues/{id}", VENUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Bar One"))
                .andExpect(jsonPath("$.category").value("entertainment"));
    }

    @Test
    void getVenueById_whenVenueIsMissing_thenReturnNotFound() throws Exception {
        given(venueService.getVenueById(VENUE_ID))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/venues/{id}", VENUE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Venue not found"));
    }

    private VenuePublicResponse venueResponse() {
        return new VenuePublicResponse()
                .id(VENUE_ID)
                .name("Bar One")
                .address("Kazan Center, 2")
                .lat(55.7905)
                .lng(49.1140)
                .category(VenuePublicResponse.CategoryEnum.ENTERTAINMENT)
                .peopleNow(0)
                .promotions(List.of());
    }
}
