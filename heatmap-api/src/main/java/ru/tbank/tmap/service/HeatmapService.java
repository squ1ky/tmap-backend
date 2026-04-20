package ru.tbank.tmap.service;

import java.util.List;
import java.util.Optional;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import ru.tbank.tmap.domain.cluster.H3Resolution;

public interface HeatmapService {

    HeatmapResponse getHeatmapClusters(
            double swLat,
            double swLng,
            double neLat,
            double neLng,
            H3Resolution resolution,
            List<String> category,
            int window
    );

    Optional<ClusterDetailsResponse> getClusterDetails(String h3Index, H3Resolution resolution);
}
