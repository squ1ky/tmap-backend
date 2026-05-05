package ru.tbank.tmap.heatmap.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryWriteRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;

@JdbcTest
@Import({
        TestcontainersConfiguration.class,
        JdbcClusterHistoryWriteRepository.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcClusterHistoryWriteRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTIVE_VENUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final UUID PENDING_VENUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444442");
    private static final long H3_RES_9 = 617422037122678783L;

    @Autowired
    private ClusterHistoryWriteRepository clusterHistoryWriteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void refreshAggregates_whenTransactionsBelongToInactiveVenues_thenAggregatesOnlyActiveVenues() {
        insertVenue(ACTIVE_VENUE_ID, "Active Cafe", "ACTIVE");
        insertVenue(PENDING_VENUE_ID, "Pending Cafe", "PENDING");
        insertTransaction(ACTIVE_VENUE_ID, new BigDecimal("500.00"));
        insertTransaction(PENDING_VENUE_ID, new BigDecimal("700.00"));

        final int updatedRows = clusterHistoryWriteRepository.refreshAggregates(
                H3Resolution.RES_9,
                Instant.parse("2026-04-17T09:00:00Z"),
                Instant.parse("2026-04-17T11:00:00Z")
        );

        assertThat(updatedRows).isEqualTo(1);
        final Integer txCount = jdbcTemplate.queryForObject(
                """
                SELECT tx_count
                FROM cluster_history
                WHERE h3_index = ? AND resolution = 9 AND category = 'FOOD'
                """,
                Integer.class,
                H3_RES_9
        );
        final BigDecimal sumAmount = jdbcTemplate.queryForObject(
                """
                SELECT sum_amount
                FROM cluster_history
                WHERE h3_index = ? AND resolution = 9 AND category = 'FOOD'
                """,
                BigDecimal.class,
                H3_RES_9
        );

        assertThat(txCount).isEqualTo(1);
        assertThat(sumAmount).isEqualByComparingTo("500.00");
    }

    private void insertVenue(final UUID id, final String name, final String status) {
        jdbcTemplate.update(
                """
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, status, updated_at
                ) VALUES (?, ?, ?, 'Kazan Center, 1', 55.7905, 49.1140, ?, 'FOOD', ?, now())
                """,
                id,
                OWNER_ID,
                name,
                H3_RES_9,
                status
        );
    }

    private void insertTransaction(final UUID venueId, final BigDecimal amount) {
        jdbcTemplate.update(
                """
                INSERT INTO transactions (
                    id, venue_id, amount, lat, lng, h3_res7, h3_res8, h3_res9, category, occurred_at
                ) VALUES (?, ?, ?, 55.7905, 49.1140, 617422037122678781, 617422037122678782, ?,
                    'FOOD', ?)
                """,
                UUID.randomUUID(),
                venueId,
                amount,
                H3_RES_9,
                java.sql.Timestamp.from(Instant.parse("2026-04-17T10:15:00Z"))
        );
    }
}
