package ru.tbank.tmap.api;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HeatmapQueryRepositoryImpl implements HeatmapQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public HeatmapQueryRepositoryImpl(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<HeatmapClusterAggregate> findClusters(
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng,
            final int resolution,
            final Instant from
    ) {
        final String h3Column = resolveH3Column(resolution);
        final String sql = """
                SELECT
                    %s AS h3_index,
                    AVG(t.lat) AS center_lat,
                    AVG(t.lng) AS center_lng,
                    COUNT(*) AS tx_count,
                    COALESCE(AVG(t.amount), 0) AS avg_check,
                    COALESCE(SUM(t.amount), 0) AS sum_amount,
                    MAX(t.occurred_at) AS updated_at
                FROM transactions t
                WHERE t.occurred_at >= :from
                  AND t.lat BETWEEN :swLat AND :neLat
                  AND t.lng BETWEEN :swLng AND :neLng
                  AND %s IS NOT NULL
                GROUP BY %s
                ORDER BY COUNT(*) DESC, SUM(t.amount) DESC, %s
                """.formatted(h3Column, h3Column, h3Column, h3Column);

        final MapSqlParameterSource params = new MapSqlParameterSource(Map.of(
                "from", Timestamp.from(from),
                "swLat", swLat,
                "swLng", swLng,
                "neLat", neLat,
                "neLng", neLng
        ));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new HeatmapClusterAggregate(
                rs.getLong("h3_index"),
                rs.getDouble("center_lat"),
                rs.getDouble("center_lng"),
                rs.getInt("tx_count"),
                defaultIfNull(rs.getBigDecimal("avg_check")),
                defaultIfNull(rs.getBigDecimal("sum_amount")),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public Optional<ClusterDetailsAggregate> findClusterDetails(
            final long h3Index,
            final int resolution,
            final Instant from
    ) {
        final String h3Column = resolveH3Column(resolution);
        final String sql = """
                SELECT
                    COUNT(*) AS tx_count,
                    COALESCE(AVG(t.amount), 0) AS avg_check,
                    COALESCE(SUM(t.amount), 0) AS sum_amount,
                    MAX(t.occurred_at) AS updated_at
                FROM transactions t
                WHERE t.occurred_at >= :from
                  AND %s = :h3Index
                """.formatted(h3Column);

        final MapSqlParameterSource params = new MapSqlParameterSource(Map.of(
                "from", Timestamp.from(from),
                "h3Index", h3Index
        ));

        return jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next() || rs.getTimestamp("updated_at") == null) {
                return Optional.empty();
            }

            return Optional.of(new ClusterDetailsAggregate(
                    h3Index,
                    resolution,
                    rs.getInt("tx_count"),
                    defaultIfNull(rs.getBigDecimal("avg_check")),
                    defaultIfNull(rs.getBigDecimal("sum_amount")),
                    rs.getTimestamp("updated_at").toInstant()
            ));
        });
    }

    private String resolveH3Column(final int resolution) {
        return switch (resolution) {
            case 7 -> "t.h3_res7";
            case 8 -> "t.h3_res8";
            case 9 -> "t.h3_res9";
            default -> throw new IllegalArgumentException("Unsupported resolution: " + resolution);
        };
    }

    private BigDecimal defaultIfNull(final BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
