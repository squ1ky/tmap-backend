package ru.tbank.tmap.venue.application.service.business.photo;

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
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.VenueTestFactory;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class BusinessVenuePhotoUpdaterTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String NEW_KEY = "venues/" + VENUE_ID + "/new.jpg";
    private static final String OLD_KEY = "venues/" + VENUE_ID + "/old.jpg";

    @Mock
    private VenueRepository venueRepository;

    private BusinessVenuePhotoUpdater venuePhotoUpdater;

    @BeforeEach
    void setUp() {
        venuePhotoUpdater = new BusinessVenuePhotoUpdater(venueRepository);
    }

    @Test
    void swapPhotoKey_whenVenueIsActive_thenMovesToPendingUpdateAndReturnsOldKey() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
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
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.PENDING, null);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        final String returnedOldKey = venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(returnedOldKey).isNull();
        assertThat(existing.getPhotoObjectKey()).isEqualTo(NEW_KEY);
        assertThat(existing.getStatus()).isEqualTo(VenueStatus.PENDING);
    }

    @Test
    void swapPhotoKey_whenVenueIsRejected_thenKeepsStatus() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.REJECTED, null);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(existing.getStatus()).isEqualTo(VenueStatus.REJECTED);
    }

    @Test
    void swapPhotoKey_whenVenueIsAlreadyPendingUpdate_thenKeepsStatus() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.PENDING_UPDATE, null);
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
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        existing.setPhotoObjectKey(OLD_KEY);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        final String returnedOldKey = venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(returnedOldKey).isEqualTo(OLD_KEY);
        assertThat(existing.getPhotoObjectKey()).isNull();
    }

    @Test
    void clearPhotoKey_whenVenueHadNoPhoto_thenReturnsNull() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(existing));

        final String returnedOldKey = venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(returnedOldKey).isNull();
        assertThat(existing.getPhotoObjectKey()).isNull();
    }

    @Test
    void clearPhotoKey_whenVenueIsActive_thenDoesNotChangeStatus() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
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
}