package ru.tbank.tmap.service;

import java.util.List;
import java.util.Optional;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.geo.H3Resolution;

public interface HeatmapService {
    HeatmapResponse getHeatmapClusters(
            BoundingBox boundingBox,
            H3Resolution resolution,
            List<String> category,
            int window
    );
    Optional<ClusterDetailsResponse> getClusterDetails(String h3Index, H3Resolution resolution);
}
