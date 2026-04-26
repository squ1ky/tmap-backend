package ru.tbank.tmap.heatmap.repository;

import com.uber.h3core.H3Core;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.uber.h3core.util.LatLng;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.heatmap.cluster.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.HeatmapClusterAggregate;

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
            final BoundingBox boundingBox,
            final H3Resolution resolution,
            final List<VenueCategory> category,
            final Instant from) {
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
                .addValue("resolution", resolution.getValue());

        if (category != null && !category.isEmpty()) {
            sql.append("\n  AND ch.category IN (:categories)");
            params.addValue("categories", category.stream()
                    .map(Enum::name)
                    .toList()
            );
        }

        sql.append("""

                GROUP BY ch.h3_index
                ORDER BY SUM(ch.tx_count) DESC, SUM(ch.sum_amount) DESC, ch.h3_index
                """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> toClusterAggregate(rs))
                .stream()
                .filter(cluster -> boundingBox.contains(cluster.centerLat(), cluster.centerLng()))
                .toList();
    }

    @Override
    public Optional<ClusterDetailsAggregate> findClusterDetails(
            final long h3Index,
            final H3Resolution resolution,
            final Instant from) {
        final String sql = """
                WITH aggregated AS (
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
                ),
                latest_bucket AS (
                    SELECT
                        ch.category AS category,
                        ch.hour_bucket AS hour_bucket
                    FROM cluster_history ch
                    WHERE ch.hour_bucket >= :from
                      AND ch.h3_index = :h3Index
                      AND ch.resolution = :resolution
                    ORDER BY ch.hour_bucket DESC, ch.category ASC
                    LIMIT 1
                )
                SELECT
                    d.name AS district_name,
                    d.photo_url AS district_image_url,
                    lb.category AS category,
                    lb.hour_bucket AS hour_bucket,
                    a.tx_count AS tx_count,
                    a.avg_check AS avg_check,
                    a.sum_amount AS sum_amount,
                    a.updated_at AS updated_at
                FROM aggregated a
                JOIN h3_to_district hd
                  ON hd.h3_index = :h3Index
                 AND hd.resolution = :resolution
                JOIN districts d
                  ON d.id = hd.district_id
                LEFT JOIN latest_bucket lb
                  ON true
                """;

        final MapSqlParameterSource params = new MapSqlParameterSource(Map.of(
                "from", Timestamp.from(from),
                "h3Index", h3Index,
                "resolution", resolution.getValue()));

        return jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next() || rs.getTimestamp("updated_at") == null) {
                return Optional.empty();
            }

            return Optional.of(new ClusterDetailsAggregate(
                    h3Index,
                    resolution,
                    rs.getString("district_name"),
                    rs.getString("district_image_url"),
                    rs.getString("category"),
                    rs.getTimestamp("hour_bucket").toInstant(),
                    rs.getInt("tx_count"),
                    rs.getBigDecimal("avg_check"),
                    rs.getBigDecimal("sum_amount"),
                    rs.getTimestamp("updated_at").toInstant()));
        });
    }

    private HeatmapClusterAggregate toClusterAggregate(final java.sql.ResultSet rs) throws java.sql.SQLException {
        final long h3Index = rs.getLong("h3_index");
        final LatLng center = h3Core.cellToLatLng(h3Index);
        return new HeatmapClusterAggregate(
                h3Index,
                center.lat,
                center.lng,
                rs.getInt("tx_count"),
                rs.getBigDecimal("avg_check"),
                rs.getBigDecimal("sum_amount"),
                rs.getTimestamp("updated_at").toInstant());
    }
}
