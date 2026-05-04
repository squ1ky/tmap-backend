package ru.tbank.tmap.heatmap.domain;

import ru.tbank.tmap.shared.geo.H3Resolution;

import java.time.Instant;

/**
 * Repository for writing to cluster_history.
 * Separate from {@link ClusterHistoryQueryRepository} to keep read and write paths distinct.
 */
public interface ClusterHistoryWriteRepository {

    /**
     * Recalculates the aggregates for transactions within the [from, to) window for a single resolution
     * and performs a UPSERT on the result in cluster_history.
     */
    int refreshAggregates(H3Resolution resolution, Instant from, Instant to);
}
