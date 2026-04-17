package ru.tbank.tmap.heatmap.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.config.SecurityConfig;
import ru.tbank.tmap.heatmap.api.dto.ClusterDetailsResponse;
import ru.tbank.tmap.heatmap.api.dto.HeatmapClusterResponse;

@WebMvcTest(HeatmapController.class)
@Import(SecurityConfig.class)
class HeatmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HeatmapService heatmapService;

    @Test
    void shouldReturnPublicHeatmapClusters() throws Exception {
        given(heatmapService.getHeatmapClusters(55.7481, 49.0664, 55.8402, 49.1912, 8, 60))
                .willReturn(List.of(new HeatmapClusterResponse(
                        "89115b22b0bffff",
                        55.796127,
                        49.106414,
                        128,
                        new BigDecimal("742.50"),
                        new BigDecimal("95040.00"),
                        Instant.parse("2026-04-17T10:20:00Z")
                )));

        mockMvc.perform(get("/heatmap/clusters")
                        .param("swLat", "55.7481")
                        .param("swLng", "49.0664")
                        .param("neLat", "55.8402")
                        .param("neLng", "49.1912")
                        .param("resolution", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].h3Index").value("89115b22b0bffff"))
                .andExpect(jsonPath("$[0].centerLat").value(55.796127))
                .andExpect(jsonPath("$[0].centerLng").value(49.106414))
                .andExpect(jsonPath("$[0].txCount").value(128))
                .andExpect(jsonPath("$[0].avgCheck").value(742.50))
                .andExpect(jsonPath("$[0].sumAmount").value(95040.00))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-04-17T10:20:00Z"));
    }

    @Test
    void shouldReturnClusterDetailsWithoutAuthentication() throws Exception {
        given(heatmapService.getClusterDetails("89115b22b0bffff", 9))
                .willReturn(java.util.Optional.of(new ClusterDetailsResponse(
                        "89115b22b0bffff",
                        9,
                        128,
                        new BigDecimal("742.50"),
                        new BigDecimal("95040.00"),
                        Instant.parse("2026-04-17T10:20:00Z")
                )));

        mockMvc.perform(get("/heatmap/clusters/89115b22b0bffff")
                        .param("resolution", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index").value("89115b22b0bffff"))
                .andExpect(jsonPath("$.resolution").value(9))
                .andExpect(jsonPath("$.txCount").value(128))
                .andExpect(jsonPath("$.avgCheck").value(742.50))
                .andExpect(jsonPath("$.sumAmount").value(95040.00))
                .andExpect(jsonPath("$.updatedAt").value("2026-04-17T10:20:00Z"));
    }
}
