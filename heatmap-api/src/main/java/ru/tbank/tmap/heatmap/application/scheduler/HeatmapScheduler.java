package ru.tbank.tmap.heatmap.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.heatmap.application.service.ClusterHistoryAggregator;
import ru.tbank.tmap.heatmap.application.service.AnomalyDetector;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeatmapScheduler {

    private static final int WINDOW_UPPER_BOUND_OFFSET_HOURS = 1;

    private final ClusterHistoryAggregator historyAggregator;
    private final AnomalyDetector anomalyDetector;
    private final Clock clock;

    @Value("${app.aggregation.lookback-hours:1}")
    private int lookbackHours;

    @Scheduled(
            fixedDelayString = "${app.aggregation.interval-ms:60000}",
            initialDelayString = "${app.aggregation.initial-delay-ms}"
    )
    public void refresh() {
        final Instant now = Instant.now(clock);
        final Instant currentHour = now.truncatedTo(ChronoUnit.HOURS);
        final Instant from = currentHour.minus(lookbackHours, ChronoUnit.HOURS);
        final Instant to = currentHour.plus(WINDOW_UPPER_BOUND_OFFSET_HOURS, ChronoUnit.HOURS);

        final long started = System.currentTimeMillis();

        int rowsUpserted = 0;
        try {
            rowsUpserted = historyAggregator.aggregate(from, to);
        } catch (RuntimeException e) {
            log.error("Aggregation failed for window=[{}..{})", from, to, e);
            return;
        }

        int anomaliesDetected = 0;
        try {
            anomaliesDetected = anomalyDetector.detectFor(currentHour);
        } catch (RuntimeException e) {
            log.error("Anomaly detection failed for hour={}, will retry next cycle",
                    currentHour, e);
        }

        final long elapsed = System.currentTimeMillis() - started;
        log.info("Heatmap refresh: window=[{}..{}), rowsUpserted={}, anomaliesDetected={}, tookMs={}",
                from, to, rowsUpserted, anomaliesDetected, elapsed);
    }
}
