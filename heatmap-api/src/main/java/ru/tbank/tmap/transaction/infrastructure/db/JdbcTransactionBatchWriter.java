package ru.tbank.tmap.transaction.infrastructure.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
import ru.tbank.tmap.transaction.application.port.TransactionWriter;
import ru.tbank.tmap.transaction.domain.Transaction;

import java.sql.Timestamp;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JdbcTransactionBatchWriter implements TransactionWriter {

    private static final String INSERT_SQL = """
        INSERT INTO transactions
            (id, venue_id, amount, lat, lng, h3_res6, h3_res7, h3_res8, h3_res9, category, occurred_at)
        VALUES
            (:id, :venueId, :amount, :lat, :lng, :h3Res6, :h3Res7, :h3Res8, :h3Res9, :category, :occurredAt)
        ON CONFLICT DO NOTHING
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final H3IndexService h3IndexService;

    @Override
    public int insertBatch(List<Transaction> rows) {
        if (rows.isEmpty()) {
            return 0;
        }

        SqlParameterSource[] parameters = rows.stream()
                .map(this::toRow)
                .map(JdbcTransactionBatchWriter::toParams)
                .toArray(SqlParameterSource[]::new);

        int[] updatedCounts = jdbcTemplate.batchUpdate(INSERT_SQL, parameters);
        int inserted = 0;
        for (int count : updatedCounts) {
            if (count > 0) {
                inserted += count;
            }
        }
        log.debug("Batch insert: attempted={}, inserted={}", rows.size(), inserted);
        return inserted;
    }

    private TransactionRow toRow(Transaction transaction) {
        long h3Res9 = h3IndexService.toH3(transaction.lat(), transaction.lng(), H3Resolution.RES_9);
        long h3Res8 = h3IndexService.cellToParent(h3Res9, H3Resolution.RES_8);
        long h3Res7 = h3IndexService.cellToParent(h3Res8, H3Resolution.RES_7);
        long h3Res6 = h3IndexService.cellToParent(h3Res7, H3Resolution.RES_6);

        return new TransactionRow(
                transaction.id(),
                transaction.venueId(),
                transaction.amount(),
                transaction.lat(),
                transaction.lng(),
                h3Res6,
                h3Res7,
                h3Res8,
                h3Res9,
                transaction.category(),
                transaction.occurredAt()
        );
    }

    private static SqlParameterSource toParams(TransactionRow r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id())
                .addValue("venueId", r.venueId())
                .addValue("amount", r.amount())
                .addValue("lat", r.lat())
                .addValue("lng", r.lng())
                .addValue("h3Res6", r.h3Res6())
                .addValue("h3Res7", r.h3Res7())
                .addValue("h3Res8", r.h3Res8())
                .addValue("h3Res9", r.h3Res9())
                .addValue("category", r.category().name())
                .addValue("occurredAt", Timestamp.from(r.occurredAt()));
    }
}
