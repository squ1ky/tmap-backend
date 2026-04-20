package ru.tbank.tmap.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.domain.cluster.H3Resolution;
import ru.tbank.tmap.repository.ClusterHistoryWriteRepository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class JdbcClusterHistoryWriteRepository implements ClusterHistoryWriteRepository {

    private static final String UPSERT_SQL_TEMPLATE = """
            INSERT INTO cluster_history
                (h3_index, resolution, category, hour_bucket,
                 tx_count, avg_check, sum_amount)
            SELECT
                %s                                    AS h3_index,
                :resolution                           AS resolution,
                t.category                            AS category,
                date_trunc('hour', t.occurred_at)     AS hour_bucket,
                count(*)                              AS tx_count,
                avg(t.amount)                         AS avg_check,
                sum(t.amount)                         AS sum_amount
            FROM transactions t
            WHERE t.occurred_at >= :fromTs
              AND t.occurred_at <  :toTs
            GROUP BY %s, t.category, date_trunc('hour', t.occurred_at)
            ON CONFLICT (h3_index, resolution, category, hour_bucket) DO UPDATE
            SET tx_count   = EXCLUDED.tx_count,
                avg_check  = EXCLUDED.avg_check,
                sum_amount = EXCLUDED.sum_amount
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public int refreshAggregates(H3Resolution resolution, Instant from, Instant to) {
        String column = resolveH3Column(resolution);
        String sql = String.format(UPSERT_SQL_TEMPLATE, column, column);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("resolution", resolution.getValue())
                .addValue("fromTs", Timestamp.from(from))
                .addValue("toTs", Timestamp.from(to));

        return jdbcTemplate.update(sql, params);
    }

    private static String resolveH3Column(H3Resolution resolution) {
        return switch (resolution) {
            case RES_7 -> "t.h3_res7";
            case RES_8 -> "t.h3_res8";
            case RES_9 -> "t.h3_res9";
        };
    }
}
