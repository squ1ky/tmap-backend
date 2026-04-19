package ru.tbank.tmap.repository.jdbc;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.repository.HeatmapQueryRepository;
import ru.tbank.tmap.repository.model.ClusterDetailsAggregate;
import ru.tbank.tmap.repository.model.HeatmapClusterAggregate;

@Repository
public class JdbcHeatmapQueryRepository implements HeatmapQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final H3Core h3Core;

    public JdbcHeatmapQueryRepository(final NamedParameterJdbcTemplate jdbcTemplate, final H3Core h3Core) {
        this.jdbcTemplate = jdbcTemplate;
        this.h3Core = h3Core;
    }

    @Override
    public List<HeatmapClusterAggregate> findClusters(
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng,
            final int resolution,
            final List<String> category,
            final Instant from
    ) {
        final StringBuilder sql = new StringBuilder(640).append("""
                SELECT
                    ch.h3_index AS h3_index,
                    COALESCE(SUM(ch.tx_count), 0) AS tx_count,
                    CASE
                        WHEN COALESCE(SUM(ch.tx_count), 0) = 0 THEN 0
                        ELSE COALESCE(SUM(ch.sum_amount), 0) / SUM(ch.tx_count)
                    END AS avg_check,
                    COALESCE(SUM(ch.sum_amount), 0) AS sum_amount,
                    MAX(ch.hour_bucket) AS updated_at
                FROM cluster_history ch
                WHERE ch.hour_bucket >= :from
                  AND ch.resolution = :resolution
                """);

        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("resolution", resolution);

        if (category != null && !category.isEmpty()) {
            sql.append("\n  AND ch.category IN (:categories)");
            params.addValue("categories", category);
        }

        sql.append("""

                GROUP BY ch.h3_index
                ORDER BY SUM(ch.tx_count) DESC, SUM(ch.sum_amount) DESC, ch.h3_index
                """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> toClusterAggregate(rs))
                .stream()
                .filter(cluster -> isWithinViewport(
                        cluster.centerLat(),
                        cluster.centerLng(),
                        swLat,
                        swLng,
                        neLat,
                        neLng
                ))
                .toList();
    }

    @Override
    public Optional<ClusterDetailsAggregate> findClusterDetails(
            final long h3Index,
            final int resolution,
            final Instant from
    ) {
        final String sql = """
                SELECT
                    COALESCE(SUM(ch.tx_count), 0) AS tx_count,
                    CASE
                        WHEN COALESCE(SUM(ch.tx_count), 0) = 0 THEN 0
                        ELSE COALESCE(SUM(ch.sum_amount), 0) / SUM(ch.tx_count)
                    END AS avg_check,
                    COALESCE(SUM(ch.sum_amount), 0) AS sum_amount,
                    MAX(ch.hour_bucket) AS updated_at
                FROM cluster_history ch
                WHERE ch.hour_bucket >= :from
                  AND ch.h3_index = :h3Index
                  AND ch.resolution = :resolution
                """;

        final MapSqlParameterSource params = new MapSqlParameterSource(Map.of(
                "from", Timestamp.from(from),
                "h3Index", h3Index,
                "resolution", resolution
        ));

        return jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next() || rs.getTimestamp("updated_at") == null) {
                return Optional.empty();
            }

            return Optional.of(new ClusterDetailsAggregate(
                    h3Index,
                    resolution,
                    rs.getInt("tx_count"),
                    rs.getBigDecimal("avg_check"),
                    rs.getBigDecimal("sum_amount"),
                    rs.getTimestamp("updated_at").toInstant()
            ));
        });
    }

    private HeatmapClusterAggregate toClusterAggregate(final java.sql.ResultSet rs) throws java.sql.SQLException {
        final long h3Index = rs.getLong("h3_index");
        final GeoCoord center = h3Core.h3ToGeo(h3Index);
        return new HeatmapClusterAggregate(
                h3Index,
                center.lat,
                center.lng,
                rs.getInt("tx_count"),
                rs.getBigDecimal("avg_check"),
                rs.getBigDecimal("sum_amount"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private boolean isWithinViewport(
            final double lat,
            final double lng,
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng
    ) {
        final boolean withinLatitude = lat >= swLat && lat <= neLat;
        final boolean withinLongitude = swLng <= neLng
                ? lng >= swLng && lng <= neLng
                : lng >= swLng || lng <= neLng;
        return withinLatitude && withinLongitude;
    }
}
