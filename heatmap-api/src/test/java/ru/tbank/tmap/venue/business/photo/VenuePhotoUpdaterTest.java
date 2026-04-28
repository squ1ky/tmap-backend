package ru.tbank.tmap.venue.business.photo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRole;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class VenuePhotoUpdaterTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final long H3_RES_9 = 617422037122678783L;

    private static final String NEW_KEY = "venues/" + VENUE_ID + "/new.jpg";
    private static final String OLD_KEY = "venues/" + VENUE_ID + "/old.jpg";

    @Mock
    private VenueRepository venueRepository;

    private VenuePhotoUpdater venuePhotoUpdater;

    @BeforeEach
    void setUp() {
        venuePhotoUpdater = new VenuePhotoUpdater(venueRepository);
    }

    @Test
    void swapPhotoKey_whenVenueIsActive_thenMovesToPendingUpdateAndReturnsOldKey() {
        final Venue existing = venue(VenueStatus.ACTIVE);
        existing.setPhotoObjectKey(OLD_KEY);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        final String returnedOldKey = venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(returnedOldKey).isEqualTo(OLD_KEY);
        assertThat(existing.getPhotoObjectKey()).isEqualTo(NEW_KEY);
        assertThat(existing.getStatus()).isEqualTo(VenueStatus.PENDING_UPDATE);
    }

    @Test
    void swapPhotoKey_whenVenueIsPending_thenKeepsStatusAndReturnsNullOldKey() {
        final Venue existing = venue(VenueStatus.PENDING);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        final String returnedOldKey = venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(returnedOldKey).isNull();
        assertThat(existing.getPhotoObjectKey()).isEqualTo(NEW_KEY);
        assertThat(existing.getStatus()).isEqualTo(VenueStatus.PENDING);
    }

    @Test
    void swapPhotoKey_whenVenueIsRejected_thenKeepsStatus() {
        final Venue existing = venue(VenueStatus.REJECTED);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(existing.getStatus()).isEqualTo(VenueStatus.REJECTED);
    }

    @Test
    void swapPhotoKey_whenVenueIsAlreadyPendingUpdate_thenKeepsStatus() {
        final Venue existing = venue(VenueStatus.PENDING_UPDATE);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(existing.getStatus()).isEqualTo(VenueStatus.PENDING_UPDATE);
    }

    @Test
    void swapPhotoKey_whenVenueNotFound_thenThrows() {
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY))
                .isInstanceOf(VenueNotFoundException.class);
    }

    @Test
    void clearPhotoKey_whenVenueHadPhoto_thenClearsKeyAndReturnsOldKey() {
        final Venue existing = venue(VenueStatus.ACTIVE);
        existing.setPhotoObjectKey(OLD_KEY);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        final String returnedOldKey = venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(returnedOldKey).isEqualTo(OLD_KEY);
        assertThat(existing.getPhotoObjectKey()).isNull();
    }

    @Test
    void clearPhotoKey_whenVenueHadNoPhoto_thenReturnsNull() {
        final Venue existing = venue(VenueStatus.ACTIVE);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        final String returnedOldKey = venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(returnedOldKey).isNull();
        assertThat(existing.getPhotoObjectKey()).isNull();
    }

    @Test
    void clearPhotoKey_whenVenueIsActive_thenDoesNotChangeStatus() {
        final Venue existing = venue(VenueStatus.ACTIVE);
        existing.setPhotoObjectKey(OLD_KEY);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(existing.getStatus()).isEqualTo(VenueStatus.ACTIVE);
    }

    @Test
    void clearPhotoKey_whenVenueNotFound_thenThrows() {
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID))
                .isInstanceOf(VenueNotFoundException.class);
    }

    private Venue venue(final VenueStatus status) {
        final User owner = new User(
                OWNER_ID,
                "owner@example.com",
                "password-hash",
                "Owner",
                UserRole.BUSINESS_OWNER
        );
        final Venue venue = new Venue(
                VENUE_ID,
                owner,
                "Bar One",
                "Kazan Center, 2",
                GeoPoint.of(55.7905, 49.1140),
                H3_RES_9,
                VenueCategory.ENTERTAINMENT
        );
        venue.setStatus(status);
        return venue;
    }
}