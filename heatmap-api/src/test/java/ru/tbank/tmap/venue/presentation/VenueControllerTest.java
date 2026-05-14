package ru.tbank.tmap.venue.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.test.security.TestSecurityConfig;
import ru.tbank.tmap.venue.application.query.VenuePromoProjection;
import ru.tbank.tmap.venue.application.service.VenueQueryService;
import ru.tbank.tmap.venue.application.service.VenueSearchService;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.venue.application.query.VenueProjection;
import ru.tbank.tmap.venue.application.query.VenueSearchProjection;

@WebMvcTest(VenueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        VenueMapper.class
})
class VenueControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROMO_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueQueryService publicVenueService;

    @MockitoBean
    private VenueSearchService venueSearchService;

    @MockitoBean
    private MinioUrlBuilder minioUrlBuilder;

    @Test
    void getVenuesInViewport_whenRequestIsValid_thenReturnVenues() throws Exception {
        given(publicVenueService.getVenuesInViewport(
                new BoundingBox(55.7801, 49.1102, 55.7995, 49.1355),
                List.of()))
                .willReturn(List.of(venueResponse()));
        given(publicVenueService.getVenuePromosByVenueIds(List.of(VENUE_ID)))
                .willReturn(Map.of(VENUE_ID, List.of(promoResponse())));

        mockMvc.perform(get("/api/v1/venues")
                        .param("swLat", "55.7801")
                        .param("swLng", "49.1102")
                        .param("neLat", "55.7995")
                        .param("neLng", "49.1355"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Bar One"))
                .andExpect(jsonPath("$[0].category").value("entertainment"))
                .andExpect(jsonPath("$[0].promotions[0].id").value(PROMO_ID.toString()))
                .andExpect(jsonPath("$[0].promotions[0].title").value("Happy hours"));
    }

    @Test
    void getVenuesInViewport_whenCategoryIsProvided_thenPassCategoryFilter() throws Exception {
        given(publicVenueService.getVenuesInViewport(
                new BoundingBox(55.7801, 49.1102, 55.7995, 49.1355),
                List.of(VenueCategory.FOOD)))
                .willReturn(List.of());
        given(publicVenueService.getVenuePromosByVenueIds(List.of()))
                .willReturn(Map.of());

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
        given(publicVenueService.getVenueById(VENUE_ID))
                .willReturn(Optional.of(venueResponse()));
        given(publicVenueService.getVenuePromos(VENUE_ID))
                .willReturn(List.of(promoResponse()));

        mockMvc.perform(get("/api/v1/venues/{id}", VENUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Bar One"))
                .andExpect(jsonPath("$.category").value("entertainment"))
                .andExpect(jsonPath("$.promotions[0].id").value(PROMO_ID.toString()))
                .andExpect(jsonPath("$.promotions[0].title").value("Happy hours"));
    }

    @Test
    void getVenueById_whenVenueIsMissing_thenReturnNotFound() throws Exception {
        given(publicVenueService.getVenueById(VENUE_ID))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/venues/{id}", VENUE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Venue not found"));
    }

    @Test
    void searchVenues_whenRequestIsValid_thenReturnsMatchingVenues() throws Exception {
        given(venueSearchService.searchByName("bar"))
                .willReturn(List.of(new VenueSearchProjection(
                        VENUE_ID,
                        "Bar One",
                        "Kazan Center, 2",
                        55.7905,
                        49.1140,
                        VenueCategory.ENTERTAINMENT,
                        null
                )));

        mockMvc.perform(get("/api/v1/venues/search")
                        .param("q", "bar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Bar One"))
                .andExpect(jsonPath("$[0].address").value("Kazan Center, 2"))
                .andExpect(jsonPath("$[0].lat").value(55.7905))
                .andExpect(jsonPath("$[0].lng").value(49.1140))
                .andExpect(jsonPath("$[0].category").value("entertainment"));
    }

    @Test
    void searchVenues_whenNoMatches_thenReturnsEmptyArray() throws Exception {
        given(venueSearchService.searchByName("missing")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/venues/search")
                        .param("q", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private VenueProjection venueResponse() {
        return new VenueProjection(
                VENUE_ID,
                "Bar One",
                "Kazan Center, 2",
                55.7905,
                49.1140,
                null,
                VenueCategory.ENTERTAINMENT,
                null,
                null,
                null,
                null,
                null
        );
    }

    private VenuePromoProjection promoResponse() {
        return new VenuePromoProjection(
                PROMO_ID,
                VENUE_ID,
                "Happy hours",
                "20% off after 20:00",
                OffsetDateTime.parse("2026-05-10T10:00:00+03:00"),
                OffsetDateTime.parse("2026-05-20T23:00:00+03:00"),
                OffsetDateTime.parse("2026-05-01T10:00:00+03:00")
        );
    }
}
