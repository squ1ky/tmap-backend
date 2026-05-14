package ru.tbank.tmap.transaction.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.transaction.application.port.TransactionWriter;
import ru.tbank.tmap.transaction.domain.Transaction;
import ru.tbank.tmap.transaction.domain.TransactionTestFactory;
import ru.tbank.tmap.venue.api.VenueCategory;

@JdbcTest
@Import({
        TestcontainersConfiguration.class,
        JdbcTransactionBatchWriter.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcTransactionBatchWriterTest {

    private static final UUID VENUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TRANSACTION_ID_1 = UUID.fromString("55555555-5555-5555-5555-555555555551");
    private static final UUID TRANSACTION_ID_2 = UUID.fromString("55555555-5555-5555-5555-555555555552");
    private static final UUID TRANSACTION_ID_3 = UUID.fromString("55555555-5555-5555-5555-555555555553");

    @Autowired
    private TransactionWriter transactionWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Test
    void insertBatch_whenListIsEmpty_thenReturnsZero() {
        final int inserted = transactionWriter.insertBatch(List.of());

        assertThat(inserted).isZero();
        assertThat(countTransactions()).isZero();
    }

    @Test
    void insertBatch_whenSingleTransaction_thenPersistsAllFields() {
        insertVenue(VENUE_ID, VenueCategory.FOOD);
        final BigDecimal amount = new BigDecimal("250.75");
        final Instant occurredAt = Instant.parse("2025-03-10T14:30:00Z");
        final Transaction transaction = TransactionTestFactory.transaction()
                .withId(TRANSACTION_ID_1)
                .withVenueId(VENUE_ID)
                .withAmount(amount)
                .withLocation(55.7910, 49.1210)
                .withH3Indices(608111111111111111L, 613222222222222222L, 617333333333333333L)
                .withCategory(VenueCategory.FOOD)
                .withOccurredAt(occurredAt)
                .build();

        final int inserted = transactionWriter.insertBatch(List.of(transaction));

        assertThat(inserted).isEqualTo(1);
        final Map<String, Object> row = selectTransactionById(TRANSACTION_ID_1);
        assertThat(row.get("id")).isEqualTo(TRANSACTION_ID_1);
        assertThat(row.get("venue_id")).isEqualTo(VENUE_ID);
        assertThat(((BigDecimal) row.get("amount"))).isEqualByComparingTo(amount);
        assertThat(((Number) row.get("lat")).doubleValue()).isEqualTo(55.7910);
        assertThat(((Number) row.get("lng")).doubleValue()).isEqualTo(49.1210);
        assertThat(((Number) row.get("h3_res7")).longValue()).isEqualTo(608111111111111111L);
        assertThat(((Number) row.get("h3_res8")).longValue()).isEqualTo(613222222222222222L);
        assertThat(((Number) row.get("h3_res9")).longValue()).isEqualTo(617333333333333333L);
        assertThat(row.get("category")).isEqualTo(VenueCategory.FOOD.name());
        assertThat(((Timestamp) row.get("occurred_at")).toInstant()).isEqualTo(occurredAt);
    }

    @Test
    void insertBatch_whenMultipleTransactions_thenInsertsAllRows() {
        insertVenue(VENUE_ID, VenueCategory.FOOD);
        final List<Transaction> transactions = List.of(
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_1).withVenueId(VENUE_ID).build(),
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_2).withVenueId(VENUE_ID).build(),
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_3).withVenueId(VENUE_ID).build()
        );

        final int inserted = transactionWriter.insertBatch(transactions);

        assertThat(inserted).isEqualTo(3);
        assertThat(countTransactions()).isEqualTo(3);
    }

    @Test
    void insertBatch_whenDuplicateIdInDatabase_thenIgnoresDuplicateAndInsertsOthers() {
        insertVenue(VENUE_ID, VenueCategory.FOOD);
        transactionWriter.insertBatch(List.of(
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_1).withVenueId(VENUE_ID).build()
        ));

        final int inserted = transactionWriter.insertBatch(List.of(
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_1).withVenueId(VENUE_ID).build(),
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_2).withVenueId(VENUE_ID).build(),
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_3).withVenueId(VENUE_ID).build()
        ));

        assertThat(inserted).isEqualTo(2);
        assertThat(countTransactions()).isEqualTo(3);
    }

    @Test
    void insertBatch_whenAllIdsAreDuplicates_thenReturnsZero() {
        insertVenue(VENUE_ID, VenueCategory.FOOD);
        transactionWriter.insertBatch(List.of(
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_1).withVenueId(VENUE_ID).build(),
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_2).withVenueId(VENUE_ID).build()
        ));

        final int inserted = transactionWriter.insertBatch(List.of(
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_1).withVenueId(VENUE_ID).build(),
                TransactionTestFactory.transaction().withId(TRANSACTION_ID_2).withVenueId(VENUE_ID).build()
        ));

        assertThat(inserted).isZero();
        assertThat(countTransactions()).isEqualTo(2);
    }

    @Test
    void insertBatch_whenCategoryIsProvided_thenPersistsCategoryAsName() {
        insertVenue(VENUE_ID, VenueCategory.SHOPPING);
        final Transaction transaction = TransactionTestFactory.transaction()
                .withId(TRANSACTION_ID_1)
                .withVenueId(VENUE_ID)
                .withCategory(VenueCategory.SHOPPING)
                .build();

        transactionWriter.insertBatch(List.of(transaction));

        final Map<String, Object> row = selectTransactionById(TRANSACTION_ID_1);
        assertThat(row.get("category")).isEqualTo("SHOPPING");
    }

    private void insertVenue(final UUID id, final VenueCategory category) {
        jdbcTemplate.update(
                """
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, description, status, updated_at
                )
                VALUES (?, ?, 'Test Venue', 'Kazan Test Address', ?, ?, ?, ?, 'Test', 'ACTIVE', now())
                """,
                id,
                OWNER_ID,
                TransactionTestFactory.DEFAULT_LAT,
                TransactionTestFactory.DEFAULT_LNG,
                TransactionTestFactory.DEFAULT_H3_RES9,
                category.name()
        );
    }

    private Map<String, Object> selectTransactionById(final UUID id) {
        return namedJdbcTemplate.queryForMap(
                "SELECT * FROM transactions WHERE id = :id",
                Map.of("id", id)
        );
    }

    private int countTransactions() {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions", Integer.class
        );
        return count == null ? 0 : count;
    }
}