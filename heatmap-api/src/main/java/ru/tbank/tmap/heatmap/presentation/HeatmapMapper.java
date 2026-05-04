package ru.tbank.tmap.heatmap.presentation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.openapitools.model.ClusterDTO;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.application.query.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.presentation.dto.HeatmapClusters;

@Component
public class HeatmapMapper {

    public HeatmapResponse toResponse(final HeatmapClusters heatmap) {
        return new HeatmapResponse()
                .generatedAt(heatmap.generatedAt())
                .refreshIntervalMinutes(heatmap.refreshIntervalMinutes())
                .aggregationWindowMinutes(heatmap.aggregationWindowMinutes())
                .clusters(heatmap.clusters().stream()
                        .map(this::toCluster)
                        .toList());
    }

    public ClusterDetailsResponse toResponse(final ClusterDetailsAggregate cluster) {
        return new ClusterDetailsResponse()
                .h3Index(Long.toHexString(cluster.h3Index()))
                .resolution(cluster.resolution().getValue())
                .districtName(cluster.districtName())
                .districtImageUrl(cluster.districtImageUrl())
                .hourBucket(OffsetDateTime.ofInstant(cluster.hourBucket(), ZoneOffset.UTC))
                .txCount(cluster.txCount())
                .avgCheck(cluster.avgCheck().doubleValue())
                .sumAmount(cluster.sumAmount().doubleValue())
                .createdAt(OffsetDateTime.ofInstant(cluster.updatedAt(), ZoneOffset.UTC));
    }

    private ClusterDTO toCluster(final HeatmapClusterAggregate cluster) {
        return new ClusterDTO(
                Long.toHexString(cluster.h3Index()),
                cluster.txCount(),
                cluster.avgCheck().doubleValue()
        );
    }
}
