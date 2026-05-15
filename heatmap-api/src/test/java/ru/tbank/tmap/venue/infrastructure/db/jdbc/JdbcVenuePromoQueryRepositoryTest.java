package ru.tbank.tmap.venue.infrastructure.db.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.venue.application.query.VenuePromoProjection;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.repository.VenuePromoQueryRepository;

@JdbcTest
@Import({TestcontainersConfiguration.class, JdbcVenuePromoQueryRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JdbcVenuePromoQueryRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private VenuePromoQueryRepository venuePromoQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findActiveByVenueId_returnsOnlyCurrentPromos() {
        final UUID activeVenueId = UUID.randomUUID();
        final UUID activePromoId = UUID.randomUUID();
        final UUID futurePromoId = UUID.randomUUID();
        final UUID expiredPromoId = UUID.randomUUID();

        insertVenue(activeVenueId, "ACTIVE");
        insertPromo(activePromoId, activeVenueId, now().minusDays(2), now().plusDays(2), now().minusDays(3));
        insertPromo(futurePromoId, activeVenueId, now().plusDays(1), now().plusDays(2), now().minusDays(1));
        insertPromo(expiredPromoId, activeVenueId, now().minusDays(3), now().minusHours(1), now().minusDays(4));

        final List<VenuePromoProjection> result = venuePromoQueryRepository.findActiveByVenueId(activeVenueId);

        assertThat(result)
                .extracting(VenuePromoProjection::id)
                .containsExactly(activePromoId);
    }

    @Test
    void findActiveByVenueIds_groupsByVenueAndSkipsInactiveVenue() {
        final UUID activeVenueId = UUID.randomUUID();
        final UUID secondActiveVenueId = UUID.randomUUID();
        final UUID pendingVenueId = UUID.randomUUID();
        final UUID activePromoId = UUID.randomUUID();
        final UUID secondVenuePromoId = UUID.randomUUID();
        final UUID pendingVenuePromoId = UUID.randomUUID();

        insertVenue(activeVenueId, "ACTIVE");
        insertVenue(secondActiveVenueId, "ACTIVE");
        insertVenue(pendingVenueId, "PENDING");

        insertPromo(activePromoId, activeVenueId, null, null, now().minusDays(2));
        insertPromo(secondVenuePromoId, secondActiveVenueId, now().minusDays(1), now().plusDays(1), now().minusDays(1));
        insertPromo(pendingVenuePromoId, pendingVenueId, null, null, now().minusDays(1));

        final Map<UUID, List<VenuePromoProjection>> result = venuePromoQueryRepository.findActiveByVenueIds(
                List.of(activeVenueId, secondActiveVenueId, pendingVenueId)
        );

        assertThat(result).containsOnlyKeys(activeVenueId, secondActiveVenueId);
        assertThat(result.get(activeVenueId))
                .extracting(VenuePromoProjection::id)
                .containsExactly(activePromoId);
        assertThat(result.get(secondActiveVenueId))
                .extracting(VenuePromoProjection::id)
                .containsExactly(secondVenuePromoId);
    }

    private void insertVenue(final UUID venueId, final String status) {
        jdbcTemplate.update(
                """
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, description, status, updated_at
                )
                VALUES (?, ?, ?, 'Kazan Test Address', 55.79, 49.12, ?, ?, 'Repository test venue', ?, now())
                """,
                venueId,
                OWNER_ID,
                "Venue " + venueId,
                617733123456780000L + Math.abs(venueId.hashCode()),
                VenueCategory.FOOD.name(),
                status
        );
    }

    private void insertPromo(
            final UUID promoId,
            final UUID venueId,
            final OffsetDateTime startsAt,
            final OffsetDateTime endsAt,
            final OffsetDateTime createdAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO venue_promos (
                    id, venue_id, title, description, starts_at, ends_at, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                promoId,
                venueId,
                "Promo " + promoId,
                "Test promo",
                startsAt != null ? Timestamp.from(startsAt.toInstant()) : null,
                endsAt != null ? Timestamp.from(endsAt.toInstant()) : null,
                Timestamp.from(createdAt.toInstant())
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now();
    }
}
