package ru.tbank.tmap.venue.infrastructure.db.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.venue.application.query.VenuePromoProjection;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.repository.VenuePromoQueryRepository;

@Repository
@RequiredArgsConstructor
public class JdbcVenuePromoQueryRepository implements VenuePromoQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<VenuePromoProjection> findActiveByVenueId(final UUID venueId) {
        return jdbcTemplate.query(
                baseSql() + """
                        AND vp.venue_id = :venueId
                        ORDER BY vp.starts_at NULLS FIRST, vp.created_at DESC, vp.id DESC
                        """,
                Map.of(
                        "status", VenueStatus.ACTIVE.name(),
                        "venueId", venueId
                ),
                (rs, rowNum) -> mapRow(rs)
        );
    }

    @Override
    public Map<UUID, List<VenuePromoProjection>> findActiveByVenueIds(final Collection<UUID> venueIds) {
        if (venueIds == null || venueIds.isEmpty()) {
            return Map.of();
        }

        final List<VenuePromoProjection> promos = jdbcTemplate.query(
                baseSql() + """
                        AND vp.venue_id IN (:venueIds)
                        ORDER BY vp.venue_id, vp.starts_at NULLS FIRST, vp.created_at DESC, vp.id DESC
                        """,
                Map.of(
                        "status", VenueStatus.ACTIVE.name(),
                        "venueIds", venueIds
                ),
                (rs, rowNum) -> mapRow(rs)
        );

        return promos.stream()
                .collect(Collectors.groupingBy(
                        VenuePromoProjection::venueId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private String baseSql() {
        return """
                SELECT
                    vp.id,
                    vp.venue_id AS venueId,
                    vp.title,
                    vp.description,
                    vp.starts_at AS startsAt,
                    vp.ends_at AS endsAt,
                    vp.created_at AS createdAt
                FROM venue_promos vp
                JOIN venues v ON v.id = vp.venue_id
                WHERE v.status = :status
                  AND (vp.starts_at IS NULL OR vp.starts_at <= now())
                  AND (vp.ends_at IS NULL OR vp.ends_at > now())
                """;
    }

    private VenuePromoProjection mapRow(final ResultSet rs) throws SQLException {
        return new VenuePromoProjection(
                rs.getObject("id", UUID.class),
                rs.getObject("venueId", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getObject("startsAt", OffsetDateTime.class),
                rs.getObject("endsAt", OffsetDateTime.class),
                rs.getObject("createdAt", OffsetDateTime.class)
        );
    }
}
