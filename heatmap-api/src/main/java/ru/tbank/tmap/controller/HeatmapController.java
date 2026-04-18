package ru.tbank.tmap.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.HeatmapApi;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.service.HeatmapService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HeatmapController implements HeatmapApi {

    private final HeatmapService heatmapService;

    @Override
    public ResponseEntity<HeatmapResponse> getHeatmapClusters(
            final Double swLat,
            final Double swLng,
            final Double neLat,
            final Double neLng,
            final Integer resolution,
            final List<String> category,
            final Integer window
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

    @Override
    public ResponseEntity<ClusterDetailsResponse> getClusterDetails(
            final String h3Index,
            final Integer resolution
    ) {
        return heatmapService.getClusterDetails(h3Index, resolution)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cluster not found"));
    }
}
