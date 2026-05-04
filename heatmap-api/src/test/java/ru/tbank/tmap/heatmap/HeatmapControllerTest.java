package ru.tbank.tmap.heatmap;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.application.query.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.presentation.HeatmapController;
import ru.tbank.tmap.heatmap.presentation.HeatmapMapper;
import ru.tbank.tmap.heatmap.presentation.dto.HeatmapClusters;
import ru.tbank.tmap.shared.error.GlobalExceptionHandler;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.test.security.TestSecurityConfig;

@WebMvcTest(controllers = HeatmapController.class)
@Import({
        TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        HeatmapMapper.class
})
class HeatmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HeatmapService heatmapService;

    @Test
    void getHeatmapClusters_whenRequestIsValid_thenReturnHeatmapData() throws Exception {
        given(heatmapService.getHeatmapClusters(new BoundingBox(55.7481, 49.0664, 55.8402, 49.1912),
                        H3Resolution.RES_8,
                        60))
                .willReturn(new HeatmapClusters(
                        OffsetDateTime.parse("2026-04-17T10:20:00Z"),
                        5,
                        60,
                        List.of(new HeatmapClusterAggregate(
                                Long.parseUnsignedLong("89115b22b0bffff", 16),
                                55.796127,
                                49.106414,
                                128,
                                new BigDecimal("742.50"),
                                new BigDecimal("95040.00"),
                                Instant.parse("2026-04-17T10:15:00Z")
                        ))
                ));

        mockMvc.perform(get("/api/v1/heatmap/clusters")
                        .param("swLat", "55.7481")
                        .param("swLng", "49.0664")
                        .param("neLat", "55.8402")
                        .param("neLng", "49.1912")
                        .param("resolution", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregationWindowMinutes").value(60))
                .andExpect(jsonPath("$.refreshIntervalMinutes").value(5))
                .andExpect(jsonPath("$.clusters[0].h3Index").value("89115b22b0bffff"))
                .andExpect(jsonPath("$.clusters[0].txCount").value(128))
                .andExpect(jsonPath("$.clusters[0].avgCheck").value(742.50));
    }

    @Test
    void getClusterDetails_whenClusterExists_thenReturnClusterDetails() throws Exception {
        given(heatmapService.getClusterDetails("89115b22b0bffff", H3Resolution.RES_9))
                .willReturn(Optional.of(new ClusterDetailsAggregate(
                        Long.parseUnsignedLong("89115b22b0bffff", 16),
                        H3Resolution.RES_9,
                        "Вахитовский район",
                        "",
                        Instant.parse("2026-04-17T10:00:00Z"),
                        128,
                        new BigDecimal("742.50"),
                        new BigDecimal("95040.00"),
                        Instant.parse("2026-04-17T10:15:00Z")
                )));

        mockMvc.perform(get("/api/v1/heatmap/clusters/89115b22b0bffff")
                        .param("resolution", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index").value("89115b22b0bffff"))
                .andExpect(jsonPath("$.resolution").value(9))
                .andExpect(jsonPath("$.districtName").value("Вахитовский район"))
                .andExpect(jsonPath("$.districtImageUrl").value(""))
                .andExpect(jsonPath("$.hourBucket").value("2026-04-17T10:00:00Z"))
                .andExpect(jsonPath("$.txCount").value(128))
                .andExpect(jsonPath("$.avgCheck").value(742.50))
                .andExpect(jsonPath("$.sumAmount").value(95040.00));
    }

    @Test
    void getHeatmapClusters_whenBoundsAreInvalid_thenReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/heatmap/clusters")
                        .param("swLat", "55.9")
                        .param("swLng", "49.2")
                        .param("neLat", "55.8")
                        .param("neLng", "49.1")
                        .param("resolution", "8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid map bounds"));
    }

    @Test
    void getClusterDetails_whenClusterIsMissing_thenReturnNotFoundError() throws Exception {
        given(heatmapService.getClusterDetails("89115b22b0bffff", H3Resolution.RES_9))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/heatmap/clusters/89115b22b0bffff")
                        .param("resolution", "9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cluster not found"));
    }
}
