package ru.tbank.tmap.heatmap.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.heatmap.api.dto.HeatmapResponse;

@RestController
@RequiredArgsConstructor
public class HeatmapController {

    private final HeatmapService heatmapService;

    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmap() {
        return ResponseEntity.ok(heatmapService.getHeatmap());
    }
}
