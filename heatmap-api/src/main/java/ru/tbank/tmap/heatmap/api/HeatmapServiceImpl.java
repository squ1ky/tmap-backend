package ru.tbank.tmap.heatmap.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.heatmap.api.dto.ClusterDetailsResponse;
import ru.tbank.tmap.heatmap.api.dto.HeatmapClusterResponse;

@Service
public class HeatmapServiceImpl implements HeatmapService {

    @Override
    public List<HeatmapClusterResponse> getHeatmapClusters(
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng,
            final int resolution,
            final int window
    ) {
        return List.of();
    }

    @Override
    public Optional<ClusterDetailsResponse> getClusterDetails(final String h3Index, final int resolution) {
        return Optional.of(new ClusterDetailsResponse(
                h3Index,
                resolution,
                0,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                Instant.now()
        ));
    }
}
