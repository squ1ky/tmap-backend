package ru.tbank.tmap.heatmap;

import java.util.List;
import java.util.Optional;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.venue.domain.VenueCategory;

public interface HeatmapService {
    HeatmapResponse getHeatmapClusters(
            BoundingBox boundingBox,
            H3Resolution resolution,
            List<VenueCategory> category,
            int window
    );
    Optional<ClusterDetailsResponse> getClusterDetails(String h3Index, H3Resolution resolution);
}
