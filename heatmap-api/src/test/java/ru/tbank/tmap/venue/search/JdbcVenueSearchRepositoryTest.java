package ru.tbank.tmap.venue.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.venue.application.query.VenueSearchProjection;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.repository.VenueSearchRepository;
import ru.tbank.tmap.venue.infrastructure.db.jdbc.JdbcVenueSearchRepository;

@JdbcTest
@Import({TestcontainersConfiguration.class, JdbcVenueSearchRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JdbcVenueSearchRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PHOTO_OBJECT_KEY = "/tmap/venues/photo.jpg";

    @Autowired
    private VenueSearchRepository venueSearchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchByName_whenFullNameMatches_thenReturnsVenue() {
        insertVenue("33333333-3333-3333-3333-333333333331", "Coffee Point", "ACTIVE");

        final List<VenueSearchProjection> result = venueSearchRepository.searchByName("Coffee Point");

        assertThat(result)
                .extracting(VenueSearchProjection::name)
                .contains("Coffee Point");
    }

    @Test
    void searchByName_whenPartialNameMatches_thenReturnsVenue() {
        insertVenue("33333333-3333-3333-3333-333333333332", "North Coffee Lab", "ACTIVE");

        final List<VenueSearchProjection> result = venueSearchRepository.searchByName("Coffee");

        assertThat(result)
                .extracting(VenueSearchProjection::name)
                .contains("North Coffee Lab");
    }

    @Test
    void searchByName_whenCaseDiffers_thenReturnsVenue() {
        insertVenue("33333333-3333-3333-3333-333333333333", "Case Cafe", "ACTIVE");

        final List<VenueSearchProjection> result = venueSearchRepository.searchByName("case cafe");

        assertThat(result)
                .extracting(VenueSearchProjection::name)
                .contains("Case Cafe");
    }

    @Test
    void searchByName_whenNoMatches_thenReturnsEmptyList() {
        insertVenue("33333333-3333-3333-3333-333333333334", "Hidden Cafe", "ACTIVE");

        final List<VenueSearchProjection> result = venueSearchRepository.searchByName("Cinema");

        assertThat(result).isEmpty();
    }

    @Test
    void searchByName_whenVenueIsPendingOrRejected_thenExcludesIt() {
        insertVenue("33333333-3333-3333-3333-333333333335", "Moderated Cafe Pending", "PENDING");
        insertVenue("33333333-3333-3333-3333-333333333336", "Moderated Cafe Rejected", "REJECTED");
        insertVenue("33333333-3333-3333-3333-333333333337", "Moderated Cafe Active", "ACTIVE");

        final List<VenueSearchProjection> result = venueSearchRepository.searchByName("Moderated Cafe");

        assertThat(result)
                .extracting(VenueSearchProjection::name)
                .containsExactly("Moderated Cafe Active");
    }

    @Test
    void searchByName_whenVenueMatches_thenReturnsSearchDisplayFields() {
        final UUID venueId = UUID.fromString("33333333-3333-3333-3333-333333333338");
        insertVenue(venueId.toString(), "Display Cafe", "ACTIVE");

        final VenueSearchProjection result = venueSearchRepository.searchByName("Display").getFirst();

        assertThat(result.id()).isEqualTo(venueId);
        assertThat(result.name()).isEqualTo("Display Cafe");
        assertThat(result.address()).isEqualTo("Search Street, 1");
        assertThat(result.lat()).isEqualTo(55.7961);
        assertThat(result.lng()).isEqualTo(49.1064);
        assertThat(result.category()).isEqualTo(VenueCategory.FOOD);
        assertThat(result.photoObjectKey()).isEqualTo(PHOTO_OBJECT_KEY);
    }

    private void insertVenue(final String id, final String name, final String status) {
        jdbcTemplate.update(
                """
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, photo_object_key, status
                ) VALUES (?, ?, ?, 'Search Street, 1', 55.7961, 49.1064, 617733123456789101, 'FOOD',
                    '/tmap/venues/photo.jpg', ?)
                """,
                UUID.fromString(id),
                OWNER_ID,
                name,
                status
        );
    }
}
