package ru.tbank.tmap.heatmap.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.heatmap.domain.AnomalyDetectionRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.venue.api.VenueCategory;

@JdbcTest
@Import({
        TestcontainersConfiguration.class,
        JdbcAnomalyDetectionRepository.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcAnomalyDetectionRepositoryTest {

    private static final String H3_INDEX_HEX = "89115b22b0bffff";
    private static final long H3_INDEX = Long.parseUnsignedLong(H3_INDEX_HEX, 16);

    private static final Instant CURRENT_HOUR = Instant.parse("2026-04-17T10:00:00Z");

    private static final double RATIO_THRESHOLD = 2.0;
    private static final int MIN_BASELINE = 10;
    private static final int MIN_BASELINE_DAYS = 3;

    @Autowired
    private AnomalyDetectionRepository anomalyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recompute_whenCurrentExceedsBaseline_thenInsertsAnomalyWithCorrectRatio() {
        // baseline
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 50);
        }

        // current
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 200);

        final int rowsInserted = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsInserted).isEqualTo(1);

        final AnomalyRow saved = fetchAnomaly(H3_INDEX, 9, CURRENT_HOUR);

        assertThat(saved).isNotNull();
        assertThat(saved.txCount).isEqualTo(200);
        assertThat(saved.baselineAvg).isEqualByComparingTo("50.00");
        assertThat(saved.ratio).isEqualByComparingTo("4.00");
    }

    @Test
    void recompute_whenRatioBelowThreshold_thenDoesNotInsert() {
        // baseline: 50, current: 80 -> ratio = 1.6 < RATIO_THRESHOLD
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 50);
        }
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 80);

        final int rowsInserted = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsInserted).isZero();
        assertThat(countAnomalies()).isZero();
    }

    @Test
    void recompute_whenBaselineBelowMinimum_thenDoesNotInsertEvenAtHighRatio() {
        // baseline: 5 < minBaseline = 10.
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 5);
        }
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 20);

        final int rowsInserted = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsInserted).isZero();
    }

    @Test
    void recompute_whenLessThanMinBaselineDays_thenDoesNotInsert() {
        // 2 days of cluster history <  minBaselineDays=3
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(1)), 50);
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(2)), 50);
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 200);

        final int rowsInserted = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsInserted).isZero();
    }

    @Test
    void recompute_whenBaselineFromOtherHourOfDay_thenIgnoresThemInComputation() {
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9,
                    CURRENT_HOUR.minus(Duration.ofDays(day)).plus(Duration.ofHours(2)),
                    500);
        }
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 200);

        final int rowsInserted = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsInserted).isZero();
    }

    @Test
    void recompute_whenSumsAcrossCategories_thenComputesAggregatedTxCount() {
        // 3 categories x 50 transactions = baseline 150
        for (int day = 1; day <= 7; day++) {
            final Instant pastHour = CURRENT_HOUR.minus(Duration.ofDays(day));
            insertHistory(H3_INDEX_HEX, 9, VenueCategory.FOOD, pastHour, 50);
            insertHistory(H3_INDEX_HEX, 9, VenueCategory.ENTERTAINMENT, pastHour, 50);
            insertHistory(H3_INDEX_HEX, 9, VenueCategory.SHOPPING, pastHour, 50);
        }

        // current: 100+100+100 = 300, ratio = 300/150 = 2.0
        insertHistory(H3_INDEX_HEX, 9, VenueCategory.FOOD, CURRENT_HOUR, 100);
        insertHistory(H3_INDEX_HEX, 9, VenueCategory.ENTERTAINMENT, CURRENT_HOUR, 100);
        insertHistory(H3_INDEX_HEX, 9, VenueCategory.SHOPPING, CURRENT_HOUR, 100);

        final int rowsInserted = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsInserted).isEqualTo(1);

        final AnomalyRow saved = fetchAnomaly(H3_INDEX, 9, CURRENT_HOUR);

        assertThat(saved.txCount).isEqualTo(300);
        assertThat(saved.baselineAvg).isEqualByComparingTo("150.00");
        assertThat(saved.ratio).isEqualByComparingTo("2.00");
    }

    @Test
    void recompute_whenInvokedTwice_thenIsIdempotent() {
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 50);
        }
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 200);

        anomalyRepository.recompute(H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS);
        anomalyRepository.recompute(H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS);

        assertThat(countAnomalies()).isEqualTo(1);
    }

    @Test
    void recompute_whenAnomalyExistsAndCurrentGrows_thenUpdatesRatio() {
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 50);
        }

        // current: 120, ratio: 2.4
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 120);
        anomalyRepository.recompute(H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS);

        // current: 200, ratio: 4.0
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 200);

        anomalyRepository.recompute(H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS);

        final AnomalyRow saved = fetchAnomaly(H3_INDEX, 9, CURRENT_HOUR);
        assertThat(saved.txCount).isEqualTo(200);
        assertThat(saved.ratio).isEqualByComparingTo("4.00");
    }

    @Test
    void recompute_whenResolutionDiffers_thenIgnoresOtherResolutions() {
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 8, CURRENT_HOUR.minus(Duration.ofDays(day)), 50);
        }
        insertHistory(H3_INDEX_HEX, 8, CURRENT_HOUR, 200);

        final int rowsInserted = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsInserted).isZero();
    }

    @Test
    void recompute_whenAnomalyExistsAndCurrentDrops_thenRemovesAnomaly() {
        // baseline: 50, current: 200, ratio = 4.0 -> anomaly
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 50);
        }
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 200);

        anomalyRepository.recompute(H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS);

        assertThat(countAnomalies()).isEqualTo(1);

        // current: 80, ratio = 1.6 < threshold -> no anomaly
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 80);

        final int rowsAfter = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsAfter).isZero();
        assertThat(fetchAnomaly(H3_INDEX, 9, CURRENT_HOUR)).isNull();
        assertThat(countAnomalies()).isZero();
    }

    @Test
    void recompute_whenAnomalyExistsAndBaselineGrows_thenRemovesAnomaly() {
        // baseline: 50, current: 120, ratio = 2.4 -> anomaly
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 50);
        }
        insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR, 120);

        anomalyRepository.recompute(H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS);

        assertThat(countAnomalies()).isEqualTo(1);

        // baseline: 100, current: 120
        // ratio = 1.2 < threshold -> no anomaly
        for (int day = 1; day <= 7; day++) {
            insertHistory(H3_INDEX_HEX, 9, CURRENT_HOUR.minus(Duration.ofDays(day)), 100);
        }

        final int rowsAfter = anomalyRepository.recompute(
                H3Resolution.RES_9, CURRENT_HOUR,
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );

        assertThat(rowsAfter).isZero();
        assertThat(fetchAnomaly(H3_INDEX, 9, CURRENT_HOUR)).isNull();
    }

    private void insertHistory(
            final String h3Index, final int resolution,
            final Instant hourBucket, final int txCount
    ) {
        insertHistory(h3Index, resolution, VenueCategory.FOOD, hourBucket, txCount);
    }

    private void insertHistory(
            final String h3Index, final int resolution,
            final VenueCategory category, final Instant hourBucket, final int txCount
    ) {
        final BigDecimal avgCheck = new BigDecimal("100.00");
        final BigDecimal sumAmount = avgCheck.multiply(BigDecimal.valueOf(txCount));

        jdbcTemplate.update(
                """
                        INSERT INTO cluster_history (
                            h3_index, resolution, category, hour_bucket,
                            tx_count, avg_check, sum_amount, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (h3_index, resolution, category, hour_bucket) DO UPDATE
                        SET tx_count = EXCLUDED.tx_count,
                            sum_amount = EXCLUDED.sum_amount
                        """,
                Long.parseUnsignedLong(h3Index, 16),
                resolution,
                category.name(),
                Timestamp.from(hourBucket),
                txCount,
                avgCheck,
                sumAmount,
                Timestamp.from(hourBucket)
        );
    }

    private AnomalyRow fetchAnomaly(final long h3Index, final int resolution, final Instant hourBucket) {
        final List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT tx_count, baseline_avg, ratio
                        FROM cluster_anomalies
                        WHERE h3_index = ? AND resolution = ? AND hour_bucket = ?
                        """,
                h3Index, resolution, Timestamp.from(hourBucket)
        );
        if (rows.isEmpty()) {
            return null;
        }
        final Map<String, Object> r = rows.getFirst();
        return new AnomalyRow(
                ((Number) r.get("tx_count")).intValue(),
                (BigDecimal) r.get("baseline_avg"),
                (BigDecimal) r.get("ratio")
        );
    }

    private int countAnomalies() {
        final Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cluster_anomalies", Integer.class
        );
        return c == null ? 0 : c;
    }

    private record AnomalyRow(int txCount, BigDecimal baselineAvg, BigDecimal ratio) {
    }
}