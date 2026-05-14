package ru.tbank.tmap.venue.infrastructure.db.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.application.query.VenueProjection;
import ru.tbank.tmap.venue.api.VenueCategory;
import ru.tbank.tmap.venue.domain.repository.VenueQueryRepository;

@JdbcTest
@Import({TestcontainersConfiguration.class, JdbcVenueQueryRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JdbcVenueQueryRepositoryTest {

    private static final UUID ACTIVE_FOOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333331");
    private static final UUID ACTIVE_SHOPPING_ID = UUID.fromString("33333333-3333-3333-3333-333333333332");
    private static final UUID PENDING_FOOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID REJECTED_FOOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333335");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private VenueQueryRepository venueQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findActiveInViewport_whenCategoryListIsEmpty_thenReturnsEmptyList() {
        insertVenue(ACTIVE_FOOD_ID, "Active Food", 55.7900, 49.1200, VenueCategory.FOOD, "ACTIVE");
        insertVenue(ACTIVE_SHOPPING_ID, "Active Shop", 55.7910, 49.1210, VenueCategory.SHOPPING, "ACTIVE");

        final List<VenueProjection> result = venueQueryRepository.findActiveInViewport(
                new BoundingBox(55.7800, 49.1100, 55.8000, 49.1300),
                List.of()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveInViewport_whenCategoryFilterIsProvided_thenReturnsOnlyMatchingCategory() {
        insertVenue(ACTIVE_FOOD_ID, "Active Food", 55.7900, 49.1200, VenueCategory.FOOD, "ACTIVE");
        insertVenue(ACTIVE_SHOPPING_ID, "Active Shop", 55.7910, 49.1210, VenueCategory.SHOPPING, "ACTIVE");

        final List<VenueProjection> result = venueQueryRepository.findActiveInViewport(
                new BoundingBox(55.7800, 49.1100, 55.8000, 49.1300),
                List.of(VenueCategory.FOOD)
        );

        assertThat(result)
                .extracting(VenueProjection::id)
                .contains(ACTIVE_FOOD_ID)
                .doesNotContain(ACTIVE_SHOPPING_ID);
    }

    @Test
    void findActiveById_whenVenueIsActive_thenReturnsVenue() {
        insertVenue(ACTIVE_FOOD_ID, "Active Food", 55.7900, 49.1200, VenueCategory.FOOD, "ACTIVE");

        final Optional<VenueProjection> result = venueQueryRepository.findActiveById(ACTIVE_FOOD_ID);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().id()).isEqualTo(ACTIVE_FOOD_ID);
        assertThat(result.orElseThrow().name()).isEqualTo("Active Food");
    }

    @Test
    void findActiveById_whenVenueIsNotActive_thenReturnsEmpty() {
        insertVenue(PENDING_FOOD_ID, "Pending Food", 55.7920, 49.1220, VenueCategory.FOOD, "PENDING");
        insertVenue(REJECTED_FOOD_ID, "Rejected Food", 55.7930, 49.1230, VenueCategory.FOOD, "REJECTED");

        final Optional<VenueProjection> pendingResult = venueQueryRepository.findActiveById(PENDING_FOOD_ID);
        final Optional<VenueProjection> rejectedResult = venueQueryRepository.findActiveById(REJECTED_FOOD_ID);

        assertThat(pendingResult).isEmpty();
        assertThat(rejectedResult).isEmpty();
    }

    private void insertVenue(
            final UUID id,
            final String name,
            final double lat,
            final double lng,
            final VenueCategory category,
            final String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, description, status, updated_at
                )
                VALUES (?, ?, ?, 'Kazan Test Address', ?, ?, ?, ?, 'Repository test venue', ?, now())
                """,
                id,
                OWNER_ID,
                name,
                lat,
                lng,
                617733123456780000L + Math.abs(id.hashCode()),
                category.name(),
                status
        );
    }
}
