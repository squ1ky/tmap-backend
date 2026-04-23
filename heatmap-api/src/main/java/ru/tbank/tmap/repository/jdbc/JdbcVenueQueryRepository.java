package ru.tbank.tmap.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.venue.VenueCategory;
import ru.tbank.tmap.repository.VenueQueryRepository;
import ru.tbank.tmap.repository.model.VenuePublicRow;

@Repository
public class JdbcVenueQueryRepository implements VenueQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcVenueQueryRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<VenuePublicRow> findActiveInViewport(
            final BoundingBox boundingBox,
            final List<VenueCategory> categories
    ) {
        final StringBuilder sql = baseSelect().append("""
                WHERE status = 'ACTIVE'
                  AND lat BETWEEN :swLat AND :neLat
                  AND lng BETWEEN :swLng AND :neLng
                """);
        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("swLat", boundingBox.swLat())
                .addValue("swLng", boundingBox.swLng())
                .addValue("neLat", boundingBox.neLat())
                .addValue("neLng", boundingBox.neLng());
        addCategoryFilter(sql, params, categories);
        sql.append("\nORDER BY name, id");

        return jdbcTemplate.query(sql.toString(), params, this::toVenuePublicRow);
    }

    @Override
    public Optional<VenuePublicRow> findActiveById(final UUID id) {
        final String sql = baseSelect().append("""
                WHERE status = 'ACTIVE'
                  AND id = :id
                """).toString();
        return jdbcTemplate.query(sql, Map.of("id", id), this::toOptionalVenuePublicRow);
    }

    private StringBuilder baseSelect() {
        return new StringBuilder(512).append("""
                SELECT
                    id,
                    name,
                    address,
                    lat,
                    lng,
                    description,
                    category,
                    photo_url,
                    dish_of_day,
                    music,
                    created_at,
                    updated_at
                FROM venues
                """);
    }

    private void addCategoryFilter(
            final StringBuilder sql,
            final MapSqlParameterSource params,
            final List<VenueCategory> categories
    ) {
        if (categories != null && !categories.isEmpty()) {
            sql.append("\n  AND category IN (:categories)");
            params.addValue("categories", categories.stream()
                    .map(Enum::name)
                    .toList());
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private VenuePublicRow toVenuePublicRow(final ResultSet rs, final int rowNum) throws SQLException {
        return new VenuePublicRow(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("address"),
                rs.getDouble("lat"),
                rs.getDouble("lng"),
                rs.getString("description"),
                VenueCategory.valueOf(rs.getString("category")),
                rs.getString("photo_url"),
                rs.getString("dish_of_day"),
                rs.getString("music"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private Optional<VenuePublicRow> toOptionalVenuePublicRow(final ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return Optional.empty();
        }
        return Optional.of(toVenuePublicRow(rs, 0));
    }
}
