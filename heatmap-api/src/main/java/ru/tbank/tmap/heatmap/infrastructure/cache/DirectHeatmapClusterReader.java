package ru.tbank.tmap.heatmap.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.heatmap.application.port.cache.HeatmapClusterReader;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryQueryRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "false")
@RequiredArgsConstructor
public class DirectHeatmapClusterReader implements HeatmapClusterReader {

    private final ClusterHistoryQueryRepository queryRepository;

    @Override
    public List<HeatmapClusterAggregate> read(
            final List<Long> parents,
            final H3Resolution resolution,
            final int windowMinutes,
            final Instant from,
            final Instant currentHour
    ) {
        return queryRepository.findClustersByParents(parents, resolution, from, currentHour);
    }
}
