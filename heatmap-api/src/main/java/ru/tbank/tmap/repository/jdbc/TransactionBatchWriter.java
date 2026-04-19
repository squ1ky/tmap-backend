package ru.tbank.tmap.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.repository.model.TransactionRow;

import java.sql.Timestamp;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionBatchWriter {

    private static final String INSERT_SQL = """
        INSERT INTO transactions
            (id, venue_id, amount, lat, lng, h3_res7, h3_res8, h3_res9, category, occurred_at)
        VALUES
            (:id, :venueId, :amount, :lat, :lng, :h3Res7, :h3Res8, :h3Res9, :category, :occurredAt)
        ON CONFLICT DO NOTHING
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public int insertBatch(List<TransactionRow> rows) {
        if (rows.isEmpty()) {
            return 0;
        }

        SqlParameterSource[] parameters = rows.stream()
                .map(TransactionBatchWriter::toParams)
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

    private static SqlParameterSource toParams(TransactionRow r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id())
                .addValue("venueId", r.venueId())
                .addValue("amount", r.amount())
                .addValue("lat", r.lat())
                .addValue("lng", r.lng())
                .addValue("h3Res7", r.h3Res7())
                .addValue("h3Res8", r.h3Res8())
                .addValue("h3Res9", r.h3Res9())
                .addValue("category", r.category())
                .addValue("occurredAt", Timestamp.from(r.occurredAt()));
    }
}
