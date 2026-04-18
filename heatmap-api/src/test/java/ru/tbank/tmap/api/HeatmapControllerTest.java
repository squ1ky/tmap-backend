package ru.tbank.tmap.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.openapitools.model.ClusterDTO;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.config.GlobalExceptionHandler;
import ru.tbank.tmap.config.SecurityConfig;

@WebMvcTest(HeatmapController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class HeatmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HeatmapService heatmapService;

    @Test
    void shouldReturnPublicHeatmapClusters() throws Exception {
        given(heatmapService.getHeatmapClusters(55.7481, 49.0664, 55.8402, 49.1912, 8, 60))
                .willReturn(new HeatmapResponse()
                        .aggregationWindowMinutes(60)
                        .refreshIntervalMinutes(5)
                        .clusters(List.of(new ClusterDTO(
                                "89115b22b0bffff",
                                128,
                                742.50
                        ))));

        mockMvc.perform(get("/heatmap/clusters")
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
    void shouldReturnClusterDetailsWithoutAuthentication() throws Exception {
        given(heatmapService.getClusterDetails("89115b22b0bffff", 9))
                .willReturn(Optional.of(new ClusterDetailsResponse()
                        .h3Index("89115b22b0bffff")
                        .resolution(9)
                        .txCount(128)
                        .avgCheck(742.50)
                        .sumAmount(95040.00)));

        mockMvc.perform(get("/heatmap/clusters/89115b22b0bffff")
                        .param("resolution", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index").value("89115b22b0bffff"))
                .andExpect(jsonPath("$.resolution").value(9))
                .andExpect(jsonPath("$.txCount").value(128))
                .andExpect(jsonPath("$.avgCheck").value(742.50))
                .andExpect(jsonPath("$.sumAmount").value(95040.00));
    }

    @Test
    void shouldReturnValidationErrorBody() throws Exception {
        given(heatmapService.getHeatmapClusters(55.9, 49.2, 55.8, 49.1, 8, 60))
                .willThrow(new ResponseStatusException(BAD_REQUEST, "Invalid map bounds"));

        mockMvc.perform(get("/heatmap/clusters")
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
    void shouldReturnNotFoundErrorBodyForMissingCluster() throws Exception {
        given(heatmapService.getClusterDetails("89115b22b0bffff", 9))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/heatmap/clusters/89115b22b0bffff")
                        .param("resolution", "9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cluster not found"));
    }
}
