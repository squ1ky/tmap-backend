package ru.tbank.tmap.service;

import java.util.Optional;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;

public interface HeatmapService {

    HeatmapResponse getHeatmapClusters(
            double swLat,
            double swLng,
            double neLat,
            double neLng,
            int resolution,
            int window
    );

    Optional<ClusterDetailsResponse> getClusterDetails(String h3Index, int resolution);
}
