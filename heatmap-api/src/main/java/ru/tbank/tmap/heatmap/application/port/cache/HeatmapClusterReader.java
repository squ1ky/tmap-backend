package ru.tbank.tmap.heatmap.application.port.cache;

import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.shared.geo.H3Resolution;

import java.time.Instant;
import java.util.List;

public interface HeatmapClusterReader {

    List<HeatmapClusterAggregate> read(
            List<Long> parents,
            H3Resolution resolution,
            int windowMinutes,
            Instant from,
            Instant currentHour
    );
}
