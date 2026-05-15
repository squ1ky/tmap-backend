package ru.tbank.tmap.venue.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.infrastructure.security.TestSecurityConfig;
import ru.tbank.tmap.loyalty.application.LoyaltyQrService;
import ru.tbank.tmap.loyalty.application.LoyaltyRuleService;
import ru.tbank.tmap.loyalty.application.query.LoyaltyQrView;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleDetails;
import ru.tbank.tmap.loyalty.presentation.mapper.BusinessLoyaltyRuleMapper;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.application.query.VenueProjection;
import ru.tbank.tmap.venue.application.query.VenueSearchProjection;
import ru.tbank.tmap.venue.application.service.VenueQueryService;
import ru.tbank.tmap.venue.application.service.VenueSearchService;
import ru.tbank.tmap.venue.domain.VenueCategory;

@WebMvcTest(VenueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        VenueMapper.class,
        BusinessLoyaltyRuleMapper.class
})
class VenueControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RULE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueQueryService publicVenueService;

    @MockitoBean
    private VenueSearchService venueSearchService;

    @MockitoBean
    private MinioUrlBuilder minioUrlBuilder;

    @MockitoBean
    private LoyaltyRuleService loyaltyRuleService;

    @MockitoBean
    private LoyaltyQrService loyaltyQrService;

    @Test
    void getVenuesInViewport_whenRequestIsValid_thenReturnVenues() throws Exception {
        given(publicVenueService.getVenuesInViewport(
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
                .andExpect(jsonPath("$[0].category").value("entertainment"))
                .andExpect(jsonPath("$[0].lat").value(55.7905))
                .andExpect(jsonPath("$[0].lng").value(49.1140))
                .andExpect(jsonPath("$[0].address").doesNotExist())
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].dishOfDay").doesNotExist())
                .andExpect(jsonPath("$[0].music").doesNotExist())
                .andExpect(jsonPath("$[0].photoUrl").doesNotExist())
                .andExpect(jsonPath("$[0].promotions").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());
        then(publicVenueService).should().getVenuesInViewport(
                new BoundingBox(55.7801, 49.1102, 55.7995, 49.1355),
                List.of());
        then(publicVenueService).shouldHaveNoMoreInteractions();
    }

    @Test
    void getVenuesInViewport_whenCategoryIsProvided_thenPassCategoryFilter() throws Exception {
        given(publicVenueService.getVenuesInViewport(
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
        given(publicVenueService.getVenueById(VENUE_ID))
                .willReturn(Optional.of(venueResponse()));

        mockMvc.perform(get("/api/v1/venues/{id}", VENUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Bar One"))
                .andExpect(jsonPath("$.category").value("entertainment"))
                .andExpect(jsonPath("$.promotions").isArray())
                .andExpect(jsonPath("$.promotions.length()").value(0));
    }

    @Test
    void getVenueLoyaltyRules_whenVenueExists_thenReturnsActiveRules() throws Exception {
        given(publicVenueService.getVenueById(VENUE_ID))
                .willReturn(Optional.of(venueResponse()));
        given(loyaltyRuleService.getActiveVenueRules(VENUE_ID))
                .willReturn(List.of(ruleView()));

        mockMvc.perform(get("/api/v1/venues/{id}/loyalty-rules", VENUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$[0].venueId").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$[0].discountPercent").value(15))
                .andExpect(jsonPath("$[0].remainingUsages").value(96));
    }

    @Test
    void getVenueLoyaltyQr_whenVenueExists_thenReturnsQrPayload() throws Exception {
        given(publicVenueService.getVenueById(VENUE_ID))
                .willReturn(Optional.of(venueResponse()));
        given(loyaltyQrService.issueQr(org.mockito.ArgumentMatchers.any()))
                .willReturn(new LoyaltyQrView(
                        VENUE_ID,
                        RULE_ID,
                        "lqr:1:test-token",
                        OffsetDateTime.parse("2026-05-14T12:00:00Z")));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userPrincipal(),
                null,
                userPrincipal().getAuthorities()));
        try {
            mockMvc.perform(get("/api/v1/venues/{id}/loyalty-rules/{ruleId}/qr", VENUE_ID, RULE_ID)
                            .with(user(userPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.venueId").value(VENUE_ID.toString()))
                    .andExpect(jsonPath("$.ruleId").value(RULE_ID.toString()))
                    .andExpect(jsonPath("$.qrPayload").value("lqr:1:test-token"));
        } finally {
            SecurityContextHolder.clearContext();
        }
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
                        null)));

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
                null);
    }

    private LoyaltyRuleDetails ruleView() {
        final ru.tbank.tmap.loyalty.domain.LoyaltyRule rule = new ru.tbank.tmap.loyalty.domain.LoyaltyRule(
                RULE_ID, VENUE_ID, "Discount 15%", 15, 100);
        rule.setCreatedAt(OffsetDateTime.parse("2026-05-01T10:00:00+03:00"));
        return new LoyaltyRuleDetails(rule, 4L);
    }

    private CustomUserDetails userPrincipal() {
        return new CustomUserDetails(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "user@example.com",
                "ignored",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
