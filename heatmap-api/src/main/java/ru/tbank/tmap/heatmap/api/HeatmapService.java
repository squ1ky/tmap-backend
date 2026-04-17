package ru.tbank.tmap.heatmap.api;

import java.util.List;
import java.util.Optional;
import ru.tbank.tmap.heatmap.api.dto.ClusterDetailsResponse;
import ru.tbank.tmap.heatmap.api.dto.HeatmapClusterResponse;

public interface HeatmapService {

    List<HeatmapClusterResponse> getHeatmapClusters(
            double swLat,
            double swLng,
            double neLat,
            double neLng,
            int resolution,
            int window
    );

    Optional<ClusterDetailsResponse> getClusterDetails(String h3Index, int resolution);
}
