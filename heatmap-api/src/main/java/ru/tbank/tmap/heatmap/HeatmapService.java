package ru.tbank.tmap.heatmap;

import java.util.List;
import java.util.Optional;
import ru.tbank.tmap.heatmap.cluster.ClusterDetailsAggregate;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.venue.domain.VenueCategory;

public interface HeatmapService {
    HeatmapClusters getHeatmapClusters(
            BoundingBox boundingBox,
            H3Resolution resolution,
            List<VenueCategory> category,
            int window
    );
    Optional<ClusterDetailsAggregate> getClusterDetails(String h3Index, H3Resolution resolution);
}
