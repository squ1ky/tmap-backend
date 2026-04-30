package ru.tbank.tmap.heatmap;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.MapHeatmapApi;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.heatmap.cluster.ClusterNotFoundException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HeatmapController implements MapHeatmapApi {

    private final HeatmapService heatmapService;
    private final HeatmapMapper heatmapMapper;

    @Override
    public ResponseEntity<HeatmapResponse> getHeatmapClusters(
            final Double swLat,
            final Double swLng,
            final Double neLat,
            final Double neLng,
            final Integer resolution,
            final Integer window
    ) {
        BoundingBox boundingBox = new BoundingBox(swLat, swLng, neLat, neLng);
        H3Resolution enumResolution = H3Resolution.of(resolution);

        return ResponseEntity.ok(heatmapMapper.toResponse(
                heatmapService.getHeatmapClusters(boundingBox, enumResolution, window)
        ));
    }

    @Override
    public ResponseEntity<ClusterDetailsResponse> getClusterDetails(
            final String h3Index,
            final Integer resolution
    ) {
        H3Resolution enumResolution = H3Resolution.of(resolution);

        return heatmapService.getClusterDetails(h3Index, enumResolution)
                .map(heatmapMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClusterNotFoundException(h3Index));
    }
}
