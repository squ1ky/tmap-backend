package ru.tbank.tmap.heatmap.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.math.BigDecimal;
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
import org.springframework.test.context.ActiveProfiles;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.infrastructure.h3.H3Config;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.heatmap.cluster.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.HeatmapClusterAggregate;

@JdbcTest
@Import({TestcontainersConfiguration.class, H3Config.class, JdbcHeatmapQueryRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JdbcHeatmapQueryRepositoryTest {

    private static final String RES_8_CLUSTER = "88115b22b1fffff";
    private static final String PHOTO_URL = "districts/kazan/vahitovsky.jpg";

    @Autowired
    private HeatmapQueryRepository heatmapQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private H3Core h3Core;

    @Test
    void findClusters_whenClusterHistoryContainsViewportData_thenReturnsAggregatedClusters() {
        insertClusterHistory(
                RES_8_CLUSTER,
                8,
                VenueCategory.FOOD,
                Instant.parse("2026-04-17T10:00:00Z"),
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00")
        );
        insertClusterHistory(
                RES_8_CLUSTER,
                8,
                VenueCategory.FOOD,
                Instant.parse("2026-04-17T10:15:00Z"),
                1,
                new BigDecimal("700.00"),
                new BigDecimal("700.00")
        );

        final BoundingBox boundingBox = boundingBoxAround(RES_8_CLUSTER);

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClusters(
                boundingBox,
                H3Resolution.RES_8,
                Instant.parse("2026-04-17T09:20:00Z")
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().h3Index()).isEqualTo(Long.parseUnsignedLong(RES_8_CLUSTER, 16));
        assertThat(result.getFirst().txCount()).isEqualTo(2);
        assertThat(result.getFirst().avgCheck()).isEqualByComparingTo("600.00");
        assertThat(result.getFirst().sumAmount()).isEqualByComparingTo("1200.00");
        assertThat(result.getFirst().updatedAt()).isEqualTo(Instant.parse("2026-04-17T10:15:00Z"));
        assertThat(boundingBox.contains(result.getFirst().centerLat(), result.getFirst().centerLng())).isTrue();
    }

    @Test
    void findClusterDetails_whenClusterExists_thenReturnsAggregate() {
        insertDistrictMapping(
                "89115b22b0bffff",
                9,
                "Вахитовский район",
                PHOTO_URL
        );
        insertClusterHistory(
                "89115b22b0bffff",
                9,
                VenueCategory.FOOD,
                Instant.parse("2026-04-17T10:00:00Z"),
                3,
                new BigDecimal("900.00"),
                new BigDecimal("2700.00")
        );

        final Optional<ClusterDetailsAggregate> result = heatmapQueryRepository.findClusterDetails(
                Long.parseUnsignedLong("89115b22b0bffff", 16),
                H3Resolution.RES_9,
                Instant.parse("2026-04-17T09:20:00Z")
        );

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().districtName()).isEqualTo("Вахитовский район");
        assertThat(result.orElseThrow().districtImageUrl()).isEqualTo(PHOTO_URL);
        assertThat(result.orElseThrow().hourBucket()).isEqualTo(Instant.parse("2026-04-17T10:00:00Z"));
        assertThat(result.orElseThrow().txCount()).isEqualTo(3);
        assertThat(result.orElseThrow().avgCheck()).isEqualByComparingTo("900.00");
        assertThat(result.orElseThrow().sumAmount()).isEqualByComparingTo("2700.00");
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
                districtId,
                districtName,
                photoUrl
        );

        jdbcTemplate.update(
                """
                INSERT INTO h3_to_district (h3_index, district_id, resolution)
                VALUES (?, ?, ?)
                """,
                Long.parseUnsignedLong(h3Index, 16),
                districtId,
                resolution
        );
    }

    private void insertClusterHistory(
            final String h3Index,
            final int resolution,
            final VenueCategory category,
            final Instant hourBucket,
            final int txCount,
            final BigDecimal avgCheck,
            final BigDecimal sumAmount
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO cluster_history (
                    h3_index, resolution, category, hour_bucket, tx_count, avg_check, sum_amount, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Long.parseUnsignedLong(h3Index, 16),
                resolution,
                category.name(),
                java.sql.Timestamp.from(hourBucket),
                txCount,
                avgCheck,
                sumAmount,
                java.sql.Timestamp.from(hourBucket)
        );
    }

    private BoundingBox boundingBoxAround(final String h3Index) {
        final LatLng center = h3Core.cellToLatLng(Long.parseUnsignedLong(h3Index, 16));
        return new BoundingBox(
                center.lat - 0.01,
                center.lng - 0.01,
                center.lat + 0.01,
                center.lng + 0.01
        );
    }
}
