package ru.tbank.tmap.venue.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.infrastructure.security.TestSecurityConfig;
import ru.tbank.tmap.loyalty.api.LoyaltyRuleFacade;
import ru.tbank.tmap.loyalty.application.LoyaltyQrService;
import ru.tbank.tmap.loyalty.presentation.mapper.BusinessLoyaltyRuleMapper;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.venue.application.service.VenueQueryService;
import ru.tbank.tmap.venue.application.service.VenueSearchService;

@WebMvcTest(VenueController.class)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        VenueMapper.class,
        BusinessLoyaltyRuleMapper.class
})
class VenueSecurityTest {

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
    private LoyaltyRuleFacade loyaltyRuleFacade;

    @MockitoBean
    private LoyaltyQrService loyaltyQrService;

    @Test
    void getVenueLoyaltyQr_whenRequestIsAnonymous_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/venues/{id}/loyalty-rules/{ruleId}/qr", VENUE_ID, RULE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getVenueById_whenRequestIsAnonymous_thenRemainPublic() throws Exception {
        given(publicVenueService.getVenueById(VENUE_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/venues/{id}", VENUE_ID))
                .andExpect(status().isNotFound());
    }
}
