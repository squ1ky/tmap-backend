package ru.tbank.tmap.transaction.infrastructure.db;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.transaction.application.port.TransactionCleaner;

@Component
@RequiredArgsConstructor
public class JdbcTransactionCleaner implements TransactionCleaner {

    private static final String DELETE_BY_VENUE_ID_SQL = """
        DELETE FROM transactions
        WHERE venue_id = :venueId
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void deleteByVenueId(final UUID venueId) {
        jdbcTemplate.update(
                DELETE_BY_VENUE_ID_SQL,
                new MapSqlParameterSource("venueId", venueId)
        );
    }
}
