package ru.tbank.tmap.heatmap.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.heatmap.api.dto.ClusterDetailsResponse;
import ru.tbank.tmap.heatmap.api.dto.HeatmapClusterResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HeatmapController {

    private final HeatmapService heatmapService;

    @GetMapping("/heatmap/clusters")
    public ResponseEntity<List<HeatmapClusterResponse>> getHeatmapClusters(
            @RequestParam final double swLat,
            @RequestParam final double swLng,
            @RequestParam final double neLat,
            @RequestParam final double neLng,
            @RequestParam final int resolution,
            @RequestParam(defaultValue = "60") final int window
    ) {
        return ResponseEntity.ok(heatmapService.getHeatmapClusters(
                swLat,
                swLng,
                neLat,
                neLng,
                resolution,
                window
        ));
    }

    @GetMapping("/heatmap/clusters/{h3Index}")
    public ResponseEntity<ClusterDetailsResponse> getClusterDetails(
            @PathVariable final String h3Index,
            @RequestParam final int resolution
    ) {
        return heatmapService.getClusterDetails(h3Index, resolution)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
