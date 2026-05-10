package ru.tbank.tmap.heatmap.infrastructure.db;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.heatmap.domain.AnomalyDetectionRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class JdbcAnomalyDetectionRepository implements AnomalyDetectionRepository {

    private static final String RECOMPUTE_SQL = """
            WITH current_activity AS (
                SELECT
                    ch.h3_index,
                    SUM(ch.tx_count) AS tx_count
                FROM cluster_history ch
                WHERE ch.resolution  = :resolution
                  AND ch.hour_bucket = CAST(:hourBucket AS TIMESTAMPTZ)
                GROUP BY ch.h3_index
            ),
            baseline_per_day AS (
                SELECT
                    ch.h3_index,
                    ch.hour_bucket,
                    SUM(ch.tx_count) AS tx_count
                FROM cluster_history ch
                WHERE ch.resolution = :resolution
                  AND ch.hour_bucket >= CAST(:hourBucket AS TIMESTAMPTZ) - INTERVAL '7 days'
                  AND ch.hour_bucket <  CAST(:hourBucket AS TIMESTAMPTZ)
                  AND EXTRACT(HOUR FROM ch.hour_bucket) = EXTRACT(HOUR FROM CAST(:hourBucket AS TIMESTAMPTZ))
                GROUP BY ch.h3_index, ch.hour_bucket
            ),
            baseline AS (
                SELECT
                    h3_index,
                    AVG(tx_count) AS baseline_avg,
                    COUNT(*)      AS days_with_data
                FROM baseline_per_day
                GROUP BY h3_index
            )
            INSERT INTO cluster_anomalies (
                h3_index, resolution, hour_bucket,
                tx_count, baseline_avg, ratio, computed_at
            )
            SELECT
                c.h3_index,
                :resolution,
                CAST(:hourBucket AS TIMESTAMPTZ),
                c.tx_count,
                b.baseline_avg,
                c.tx_count::numeric / NULLIF(b.baseline_avg, 0) AS ratio,
                now()
            FROM current_activity c
            JOIN baseline b USING (h3_index)
            WHERE b.days_with_data >= :minBaselineDays
              AND b.baseline_avg   >= :minBaseline
              AND c.tx_count::numeric / NULLIF(b.baseline_avg, 0) >= :ratioThreshold
            ON CONFLICT (h3_index, resolution, hour_bucket) DO UPDATE
            SET tx_count     = EXCLUDED.tx_count,
                baseline_avg = EXCLUDED.baseline_avg,
                ratio        = EXCLUDED.ratio,
                computed_at  = EXCLUDED.computed_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public int recompute(
            final H3Resolution resolution,
            final Instant hourBucket,
            final double ratioThreshold,
            final int minBaseline,
            final int minBaselineDays
    ) {
        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("resolution", resolution.getValue())
                .addValue("hourBucket", Timestamp.from(hourBucket))
                .addValue("ratioThreshold", ratioThreshold)
                .addValue("minBaseline", minBaseline)
                .addValue("minBaselineDays", minBaselineDays);

        return jdbcTemplate.update(RECOMPUTE_SQL, params);
    }
}
