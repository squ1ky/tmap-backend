package ru.tbank.tmap.heatmap;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.HeatmapApi;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.heatmap.cluster.ClusterNotFoundException;
import ru.tbank.tmap.venue.domain.VenueCategory;

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
        BoundingBox boundingBox = new BoundingBox(swLat, swLng, neLat, neLng);
        H3Resolution enumResolution = H3Resolution.of(resolution);
        List<VenueCategory> categories = null;
        if (category != null) {
            categories = category.stream()
                    .map(VenueCategory::fromString)
                    .toList();
        }

        return ResponseEntity.ok(heatmapService.getHeatmapClusters(boundingBox, enumResolution, categories, window));
    }

    @Override
    public ResponseEntity<ClusterDetailsResponse> getClusterDetails(
            final String h3Index,
            final Integer resolution
    ) {
        H3Resolution enumResolution = H3Resolution.of(resolution);

        return heatmapService.getClusterDetails(h3Index, enumResolution)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClusterNotFoundException(h3Index));
    }
}
