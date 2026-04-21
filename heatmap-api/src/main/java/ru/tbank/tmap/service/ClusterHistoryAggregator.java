package ru.tbank.tmap.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.domain.geo.H3Resolution;
import ru.tbank.tmap.repository.ClusterHistoryWriteRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterHistoryAggregator {

    private final ClusterHistoryWriteRepository writeRepository;
    private final Clock clock;

    private static final int WINDOW_UPPER_BOUND_OFFSET_HOURS = 1;

    @Value("${app.aggregation.lookback-hours:1}")
    private int lookbackHours;

    @Scheduled(
            fixedDelayString = "${app.aggregation.interval-ms:60000}",
            initialDelayString = "${app.aggregation.initial-delay-ms}"
    )
    @Transactional
    public void refresh() {
        Instant now = Instant.now(clock);
        Instant to = now.truncatedTo(ChronoUnit.HOURS).plus(WINDOW_UPPER_BOUND_OFFSET_HOURS, ChronoUnit.HOURS);
        Instant from = now.truncatedTo(ChronoUnit.HOURS).minus(lookbackHours, ChronoUnit.HOURS);

        long started = System.currentTimeMillis();
        int totalRows = 0;
        for (H3Resolution resolution : H3Resolution.values()) {
            totalRows += writeRepository.refreshAggregates(resolution, from, to);
        }
        long elapsed = System.currentTimeMillis() - started;
        log.info("Aggregation refresh: window=[{}..{}), rowsUpserted={}, tookMs={}",
                from, to, totalRows, elapsed);
    }
}
