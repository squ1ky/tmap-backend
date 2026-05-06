package ru.tbank.tmap.venue.infrastructure.db.jdbc;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.repository.VenueSearchRepository;
import ru.tbank.tmap.venue.application.query.VenueSearchProjection;

@Repository
public class JdbcVenueSearchRepository implements VenueSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataClassRowMapper<VenueSearchProjection> rowMapper =
            new DataClassRowMapper<>(VenueSearchProjection.class);

    public JdbcVenueSearchRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<VenueSearchProjection> searchByName(final String query) {
        final String sql = """
                SELECT id, name, address, lat, lng, category, photo_object_key AS photoObjectKey
                FROM venues
                WHERE status = :status
                  AND LOWER(name) LIKE CONCAT('%', LOWER(:query), '%')
                ORDER BY LOWER(name), id
                """;

        return jdbcTemplate.query(
                sql,
                Map.of("status", VenueStatus.ACTIVE.name(), "query", query),
                rowMapper
        );
    }
}
