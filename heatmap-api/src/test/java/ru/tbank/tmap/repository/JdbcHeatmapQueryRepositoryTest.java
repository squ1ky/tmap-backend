package ru.tbank.tmap.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import ru.tbank.tmap.repository.model.ClusterDetailsAggregate;
import ru.tbank.tmap.repository.model.HeatmapClusterAggregate;

@JdbcTest
@Import({TestcontainersConfiguration.class, JdbcHeatmapQueryRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JdbcHeatmapQueryRepositoryTest {

    @Autowired
    private HeatmapQueryRepository heatmapQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findClusters_whenTransactionsMatchFilters_thenReturnsAggregatedClusters() {
        final String venueId = UUID.randomUUID().toString();
        insertVenue(venueId);
        insertTransaction(
                UUID.randomUUID().toString(),
                venueId,
                "89115b22b0bffff",
                "88115b22b1fffff",
                "87115b22bffffff",
                new BigDecimal("500.00"),
                55.7900,
                49.1000,
                Instant.parse("2026-04-17T10:10:00Z")
        );
        insertTransaction(
                UUID.randomUUID().toString(),
                venueId,
                "89115b22b0bffff",
                "88115b22b1fffff",
                "87115b22bffffff",
                new BigDecimal("700.00"),
                55.8000,
                49.1200,
                Instant.parse("2026-04-17T10:15:00Z")
        );

        final List<HeatmapClusterAggregate> result = heatmapQueryRepository.findClusters(
                55.7481,
                49.0664,
                55.8402,
                49.1912,
                8,
                Instant.parse("2026-04-17T09:20:00Z")
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().h3Index()).isEqualTo(Long.parseUnsignedLong("88115b22b1fffff", 16));
        assertThat(result.getFirst().txCount()).isEqualTo(2);
        assertThat(result.getFirst().avgCheck()).isEqualByComparingTo("600.00");
        assertThat(result.getFirst().sumAmount()).isEqualByComparingTo("1200.00");
        assertThat(result.getFirst().updatedAt()).isEqualTo(Instant.parse("2026-04-17T10:15:00Z"));
    }

    @Test
    void findClusterDetails_whenClusterExists_thenReturnsAggregate() {
        insertClusterHistory(
                "89115b22b0bffff",
                9,
                "food",
                Instant.parse("2026-04-17T10:00:00Z"),
                3,
                new BigDecimal("900.00"),
                new BigDecimal("2700.00")
        );

        final Optional<ClusterDetailsAggregate> result = heatmapQueryRepository.findClusterDetails(
                Long.parseUnsignedLong("89115b22b0bffff", 16),
                9,
                Instant.parse("2026-04-17T09:20:00Z")
        );

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().txCount()).isEqualTo(3);
        assertThat(result.orElseThrow().avgCheck()).isEqualByComparingTo("900.00");
        assertThat(result.orElseThrow().sumAmount()).isEqualByComparingTo("2700.00");
    }

    private void insertVenue(final String venueId) {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, nickname, role, blocked, created_at)
                VALUES ('11111111-1111-1111-1111-111111111111', 'owner@tmap.local', 'hash', 'owner', 'USER', false, now())
                ON CONFLICT (id) DO NOTHING
                """
        );

        jdbcTemplate.update(
                """
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, status, created_at, updated_at
                ) VALUES (?, '11111111-1111-1111-1111-111111111111', 'Test Venue', 'Kazan', 55.79, 49.10,
                          ?, 'food', 'ACTIVE', now(), now())
                """,
                UUID.fromString(venueId),
                Long.parseUnsignedLong("89115b22b0bffff", 16)
        );
    }

    private void insertTransaction(
            final String transactionId,
            final String venueId,
            final String h3Res9,
            final String h3Res8,
            final String h3Res7,
            final BigDecimal amount,
            final double lat,
            final double lng,
            final Instant occurredAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO transactions (
                    id, venue_id, amount, lat, lng, h3_res7, h3_res8, h3_res9, category, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'food', ?)
                """,
                UUID.fromString(transactionId),
                UUID.fromString(venueId),
                amount,
                lat,
                lng,
                Long.parseUnsignedLong(h3Res7, 16),
                Long.parseUnsignedLong(h3Res8, 16),
                Long.parseUnsignedLong(h3Res9, 16),
                java.sql.Timestamp.from(occurredAt)
        );
    }

    private void insertClusterHistory(
            final String h3Index,
            final int resolution,
            final String category,
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
                category,
                java.sql.Timestamp.from(hourBucket),
                txCount,
                avgCheck,
                sumAmount,
                java.sql.Timestamp.from(hourBucket)
        );
    }
}
