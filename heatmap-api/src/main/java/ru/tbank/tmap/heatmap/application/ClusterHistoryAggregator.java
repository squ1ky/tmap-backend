package ru.tbank.tmap.heatmap.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryWriteRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterHistoryAggregator {

    private final ClusterHistoryWriteRepository writeRepository;

    @Transactional
    public int aggregate(final Instant from, final Instant to) {
        int total = 0;

        for (H3Resolution resolution : H3Resolution.values()) {
            total += writeRepository.refreshAggregates(resolution, from, to);
        }

        log.debug("Aggregated cluster_history: window=[{}..{}), rowsUpserted={}",
                from, to, total);
        return total;
    }
}
