package ru.tbank.tmap.heatmap.domain;

import ru.tbank.tmap.shared.geo.H3Resolution;

import java.time.Instant;

/**
 * An anomaly is detected when the tx_count for the current hour_bucket exceeds
 * the moving average for the same hour of the day over the past 7 days by more than
 * {@code ratioThreshold} times. Zones with a baseline below {@code minBaseline}
 * are filtered out to suppress noise.
 */
public interface AnomalyDetectionRepository {

    /**
     * Recalculates anomalies for a given resolution and hour.
     * <p>
     * Fully replaces the set of anomalies stored for the pair
     * ({@code resolution}, {@code hourBucket}): zones that no longer pass
     * the threshold are removed, zones that pass are inserted with current values.
     *
     * @param resolution     H3-resolution,
     * @param hourBucket     the hour for which activity is calculated (truncated to the hour)
     * @param ratioThreshold the minimum current/baseline ratio for detecting an anomaly
     * @param minBaseline    minimum baseline_avg value for zone consideration (noise filtering)
     * @param minBaselineDays minimum number of days with data in the 7-day window (cold start)
     * @return number of anomalies stored for this (resolution, hourBucket) after recomputation
     */
    int recompute(
            H3Resolution resolution,
            Instant hourBucket,
            double ratioThreshold,
            int minBaseline,
            int minBaselineDays
    );
}
