package ru.tbank.tmap.heatmap.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.uber.h3core.H3Core;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.heatmap.application.query.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryQueryRepository;
import ru.tbank.tmap.infrastructure.h3.H3Config;
import ru.tbank.tmap.shared.geo.H3Resolution;

@JdbcTest
@Import({
        TestcontainersConfiguration.class,
        H3Config.class,
        JdbcClusterHistoryQueryRepository.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcClusterHistoryQueryRepositoryTest {

    private static final String RES_8_CLUSTER = "88115b22b1fffff";
    private static final String RES_8_CLUSTER_SAME_PARENT = "88115b22b3fffff";
    private static final String RES_8_CLUSTER_OTHER_PARENT = "881f1d4881fffff";
    private static final String RES_9_CLUSTER = "89115b22b0bffff";
    private static final String PHOTO_URL = "districts/kazan/vahitovsky.jpg";

    private static final Instant FROM = Instant.parse("2026-04-17T09:20:00Z");
    private static final Instant CURRENT_HOUR = Instant.parse("2026-04-17T10:00:00Z");

    @Autowired
    private ClusterHistoryQueryRepository heatmapQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private H3Core h3Core;

    @Test
    void findClustersByParents_whenOneParentWithMultipleHourBuckets_thenAggregatesAcrossBuckets() {
        insertClusterHistory(
                RES_8_CLUSTER, 8,
                Instant.parse("2026-04-17T10:00:00Z"),
                1, new BigDecimal("500.00"), new BigDecimal("500.00")
        );
        insertClusterHistory(
                RES_8_CLUSTER, 8,
                Instant.parse("2026-04-17T10:15:00Z"),
                1, new BigDecimal("700.00"), new BigDecimal("700.00")
        );

        final long parent = parentRes6(RES_8_CLUSTER);

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClustersByParents(
                List.of(parent), H3Resolution.RES_8, FROM, CURRENT_HOUR
        );

        assertThat(result).hasSize(1);

        final HeatmapClusterAggregate cluster = result.getFirst();

        assertThat(cluster.h3Index()).isEqualTo(Long.parseUnsignedLong(RES_8_CLUSTER, 16));
        assertThat(cluster.txCount()).isEqualTo(2);
        assertThat(cluster.avgCheck()).isEqualByComparingTo("600.00");
        assertThat(cluster.sumAmount()).isEqualByComparingTo("1200.00");
        assertThat(cluster.updatedAt()).isEqualTo(Instant.parse("2026-04-17T10:15:00Z"));
        assertThat(cluster.isAnomaly()).isFalse();
        assertThat(cluster.anomalyRatio()).isNull();
    }

    @Test
    void findClustersByParents_whenMultipleParents_thenReturnsClustersFromAllParents() {
        insertClusterHistory(
                RES_8_CLUSTER, 8, CURRENT_HOUR,
                1, new BigDecimal("100.00"), new BigDecimal("100.00")
        );
        insertClusterHistory(
                RES_8_CLUSTER_OTHER_PARENT, 8, CURRENT_HOUR,
                2, new BigDecimal("200.00"), new BigDecimal("400.00")
        );

        final List<Long> parents = List.of(
                parentRes6(RES_8_CLUSTER),
                parentRes6(RES_8_CLUSTER_OTHER_PARENT)
        );

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClustersByParents(
                parents, H3Resolution.RES_8, FROM, CURRENT_HOUR
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(HeatmapClusterAggregate::h3Index)
                .containsExactlyInAnyOrder(
                        Long.parseUnsignedLong(RES_8_CLUSTER, 16),
                        Long.parseUnsignedLong(RES_8_CLUSTER_OTHER_PARENT, 16)
                );
    }

    @Test
    void findClustersByParents_whenParentNotRequested_thenFiltersOut() {
        insertClusterHistory(
                RES_8_CLUSTER, 8, CURRENT_HOUR,
                1, new BigDecimal("100.00"), new BigDecimal("100.00")
        );
        insertClusterHistory(
                RES_8_CLUSTER_OTHER_PARENT, 8, CURRENT_HOUR,
                2, new BigDecimal("200.00"), new BigDecimal("400.00")
        );

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClustersByParents(
                List.of(parentRes6(RES_8_CLUSTER)),
                H3Resolution.RES_8, FROM, CURRENT_HOUR
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().h3Index())
                .isEqualTo(Long.parseUnsignedLong(RES_8_CLUSTER, 16));
    }

    @Test
    void findClustersByParents_whenEmptyParentList_thenReturnsEmptyList() {
        insertClusterHistory(
                RES_8_CLUSTER, 8, CURRENT_HOUR,
                1, new BigDecimal("100.00"), new BigDecimal("100.00")
        );

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClustersByParents(
                List.of(), H3Resolution.RES_8, FROM, CURRENT_HOUR
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findClustersByParents_whenAnomalyExistsForCurrentHour_thenReturnsAnomalyFields() {
        insertClusterHistory(
                RES_8_CLUSTER, 8, CURRENT_HOUR,
                128, new BigDecimal("742.50"), new BigDecimal("95040.00")
        );
        insertAnomaly(
                RES_8_CLUSTER, 8, CURRENT_HOUR,
                128, new BigDecimal("37.60"), new BigDecimal("3.40")
        );

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClustersByParents(
                List.of(parentRes6(RES_8_CLUSTER)),
                H3Resolution.RES_8, FROM, CURRENT_HOUR
        );

        assertThat(result).hasSize(1);

        final HeatmapClusterAggregate cluster = result.getFirst();

        assertThat(cluster.txCount()).isEqualTo(128);
        assertThat(cluster.isAnomaly()).isTrue();
        assertThat(cluster.anomalyRatio()).isEqualByComparingTo("3.40");
    }

    @Test
    void findClustersByParents_whenSiblingsShareParent_thenBothReturned() {
        insertClusterHistory(
                RES_8_CLUSTER, 8, CURRENT_HOUR,
                1, new BigDecimal("100.00"), new BigDecimal("100.00")
        );
        insertClusterHistory(
                RES_8_CLUSTER_SAME_PARENT, 8, CURRENT_HOUR,
                3, new BigDecimal("200.00"), new BigDecimal("600.00")
        );

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClustersByParents(
                List.of(parentRes6(RES_8_CLUSTER)),
                H3Resolution.RES_8, FROM, CURRENT_HOUR
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(HeatmapClusterAggregate::h3Index)
                .containsExactlyInAnyOrder(
                        Long.parseUnsignedLong(RES_8_CLUSTER, 16),
                        Long.parseUnsignedLong(RES_8_CLUSTER_SAME_PARENT, 16)
                );
    }

    @Test
    void findClusterDetails_whenClusterExists_thenReturnsAggregate() {
        insertDistrictMapping(RES_9_CLUSTER, 9, "Вахитовский район", PHOTO_URL);
        insertClusterHistory(
                RES_9_CLUSTER, 9,
                Instant.parse("2026-04-17T10:00:00Z"),
                3, new BigDecimal("900.00"), new BigDecimal("2700.00")
        );

        final Optional<ClusterDetailsAggregate> result = heatmapQueryRepository.findClusterDetails(
                Long.parseUnsignedLong(RES_9_CLUSTER, 16),
                H3Resolution.RES_9,
                FROM,
                CURRENT_HOUR
        );

        assertThat(result).isPresent();

        final ClusterDetailsAggregate cluster = result.orElseThrow();

        assertThat(cluster.districtName()).isEqualTo("Вахитовский район");
        assertThat(cluster.districtImageUrl()).isEqualTo(PHOTO_URL);
        assertThat(cluster.hourBucket()).isEqualTo(Instant.parse("2026-04-17T10:00:00Z"));
        assertThat(cluster.txCount()).isEqualTo(3);
        assertThat(cluster.avgCheck()).isEqualByComparingTo("900.00");
        assertThat(cluster.sumAmount()).isEqualByComparingTo("2700.00");
        assertThat(cluster.isAnomaly()).isFalse();
        assertThat(cluster.anomalyRatio()).isNull();
        assertThat(cluster.baselineAvg()).isNull();
    }

    @Test
    void findClusterDetails_whenAnomalyExists_thenReturnsAnomalyFields() {
        insertDistrictMapping(RES_9_CLUSTER, 9, "Вахитовский район", PHOTO_URL);
        insertClusterHistory(
                RES_9_CLUSTER, 9, CURRENT_HOUR,
                128, new BigDecimal("742.50"), new BigDecimal("95040.00")
        );
        insertAnomaly(
                RES_9_CLUSTER, 9, CURRENT_HOUR,
                128, new BigDecimal("37.60"), new BigDecimal("3.40")
        );

        final Optional<ClusterDetailsAggregate> result = heatmapQueryRepository.findClusterDetails(
                Long.parseUnsignedLong(RES_9_CLUSTER, 16),
                H3Resolution.RES_9,
                FROM,
                CURRENT_HOUR
        );

        assertThat(result).isPresent();

        final ClusterDetailsAggregate cluster = result.orElseThrow();

        assertThat(cluster.txCount()).isEqualTo(128);
        assertThat(cluster.isAnomaly()).isTrue();
        assertThat(cluster.anomalyRatio()).isEqualByComparingTo("3.40");
        assertThat(cluster.baselineAvg()).isEqualByComparingTo("37.60");
    }

    private long parentRes6(final String h3IndexHex) {
        return h3Core.cellToParent(Long.parseUnsignedLong(h3IndexHex, 16), 6);
    }

    private void insertDistrictMapping(
            final String h3Index,
            final int resolution,
            final String districtName,
            final String photoUrl
    ) {
        final UUID districtId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO districts (id, name, city, photo_url)
                        VALUES (?, ?, 'Казань', ?)
                        """,
                districtId, districtName, photoUrl
        );

        jdbcTemplate.update(
                """
                        INSERT INTO h3_to_district (h3_index, district_id, resolution)
                        VALUES (?, ?, ?)
                        """,
                Long.parseUnsignedLong(h3Index, 16), districtId, resolution
        );
    }

    private void insertClusterHistory(
            final String h3Index,
            final int resolution,
            final Instant hourBucket,
            final int txCount,
            final BigDecimal avgCheck,
            final BigDecimal sumAmount
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO cluster_history (
                            h3_index, h3_parent_res6, resolution, hour_bucket,
                            tx_count, avg_check, sum_amount, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                Long.parseUnsignedLong(h3Index, 16),
                parentRes6(h3Index),
                resolution,
                Timestamp.from(hourBucket),
                txCount,
                avgCheck,
                sumAmount,
                Timestamp.from(hourBucket)
        );
    }

    private void insertAnomaly(
            final String h3Index,
            final int resolution,
            final Instant hourBucket,
            final int txCount,
            final BigDecimal baselineAvg,
            final BigDecimal ratio
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO cluster_anomalies (
                            h3_index, resolution, hour_bucket,
                            tx_count, baseline_avg, ratio
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                Long.parseUnsignedLong(h3Index, 16),
                resolution,
                Timestamp.from(hourBucket),
                txCount,
                baselineAvg,
                ratio
        );
    }
}
