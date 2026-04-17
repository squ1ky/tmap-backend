package ru.tbank.tmap.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.api.dto.ClusterDetailsResponse;
import ru.tbank.tmap.api.dto.HeatmapClusterResponse;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cluster not found"));
    }
}
