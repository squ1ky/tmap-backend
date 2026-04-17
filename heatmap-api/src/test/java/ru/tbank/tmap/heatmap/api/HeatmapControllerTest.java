package ru.tbank.tmap.heatmap.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.config.SecurityConfig;
import ru.tbank.tmap.heatmap.api.dto.HeatmapResponse;

@WebMvcTest(HeatmapController.class)
@Import(SecurityConfig.class)
class HeatmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HeatmapService heatmapService;

    @Test
    void shouldReturnPublicHeatmapPayload() throws Exception {
        given(heatmapService.getHeatmap())
                .willReturn(new HeatmapResponse(60, Instant.parse("2026-04-17T10:20:00Z"), List.of()));

        mockMvc.perform(get("/heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowMinutes").value(60))
                .andExpect(jsonPath("$.generatedAt").value("2026-04-17T10:20:00Z"))
                .andExpect(jsonPath("$.clusters").isArray());
    }
}
