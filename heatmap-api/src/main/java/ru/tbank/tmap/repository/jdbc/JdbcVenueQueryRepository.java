package ru.tbank.tmap.repository.jdbc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.DataClassRowMapper;
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
    private final DataClassRowMapper<VenuePublicRow> rowMapper = new DataClassRowMapper<>(VenuePublicRow.class);

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

        return jdbcTemplate.query(sql.toString(), params, rowMapper);
    }

    @Override
    public Optional<VenuePublicRow> findActiveById(final UUID id) {
        final String sql = baseSelect().append("""
                WHERE status = 'ACTIVE'
                  AND id = :id
                """).toString();
        return jdbcTemplate.query(sql, Map.of("id", id), rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(rowMapper.mapRow(rs, 0));
        });
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
                    photo_url AS photoUrl,
                    dish_of_day AS dishOfDay,
                    music,
                    created_at AS createdAt,
                    updated_at AS updatedAt
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
}
