package ru.tbank.tmap.venue.application.service.business.photo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueContent;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.VenueTestFactory;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class BusinessVenuePhotoUpdaterTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String NEW_KEY = "venues/" + VENUE_ID + "/new.jpg";
    private static final String OLD_KEY = "venues/" + VENUE_ID + "/old.jpg";

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenuePendingUpdateRepository venuePendingUpdateRepository;

    private BusinessVenuePhotoUpdater venuePhotoUpdater;

    @BeforeEach
    void setUp() {
        venuePhotoUpdater = new BusinessVenuePhotoUpdater(venueRepository, venuePendingUpdateRepository);
    }

    @Test
    void swapPhotoKey_whenVenueIsActiveAndNoPending_thenCreatesPendingAndLeavesVenueUntouched() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        existing.setPhotoObjectKey(OLD_KEY);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());

        venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(existing.getStatus()).isEqualTo(VenueStatus.ACTIVE);
        assertThat(existing.getPhotoObjectKey()).isEqualTo(OLD_KEY);

        ArgumentCaptor<VenuePendingUpdate> captor = ArgumentCaptor.forClass(VenuePendingUpdate.class);
        verify(venuePendingUpdateRepository).save(captor.capture());
        VenuePendingUpdate saved = captor.getValue();
        assertThat(saved.getVenueId()).isEqualTo(VENUE_ID);
        assertThat(saved.getPendingPhotoObjectKey()).isEqualTo(NEW_KEY);
        assertThat(saved.getStatus()).isEqualTo(VenueStatus.PENDING_UPDATE);
        assertThat(saved.getContent()).isEqualTo(existing.getContent());

        verify(venueRepository, never()).save(any());
    }

    @Test
    void swapPhotoKey_whenVenueIsActiveAndPendingExists_thenUpdatesPendingInPlace() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        existing.setPhotoObjectKey(OLD_KEY);
        final VenuePendingUpdate pending =
                VenueTestFactory.createPendingUpdate(existing, VenueStatus.PENDING_UPDATE);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.of(pending));

        venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(pending.getPendingPhotoObjectKey()).isEqualTo(NEW_KEY);
        assertThat(pending.getStatus()).isEqualTo(VenueStatus.PENDING_UPDATE);
        verify(venuePendingUpdateRepository).save(pending);

        assertThat(existing.getPhotoObjectKey()).isEqualTo(OLD_KEY);
        assertThat(existing.getStatus()).isEqualTo(VenueStatus.ACTIVE);
        verify(venueRepository, never()).save(any());
    }

    @Test
    void swapPhotoKey_whenVenueIsPending_thenReplacesPhotoDirectlyOnVenue() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.PENDING, null);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));

        venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(existing.getPhotoObjectKey()).isEqualTo(NEW_KEY);
        assertThat(existing.getStatus()).isEqualTo(VenueStatus.PENDING);
        verify(venueRepository).save(existing);
        verify(venuePendingUpdateRepository, never()).save(any());
    }

    @Test
    void swapPhotoKey_whenVenueIsRejected_thenReplacesPhotoAndResubmitsForModeration() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.REJECTED, "blurry photo");
        existing.setPhotoObjectKey(OLD_KEY);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));

        venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThat(existing.getPhotoObjectKey()).isEqualTo(NEW_KEY);
        assertThat(existing.getStatus()).isEqualTo(VenueStatus.PENDING);
        assertThat(existing.getRejectReason()).isNull();
        verify(venueRepository).save(existing);
    }

    @Test
    void swapPhotoKey_whenVenueNotFound_thenThrows() {
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> venuePhotoUpdater.swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY))
                .isInstanceOf(VenueNotFoundException.class);
    }

    @Test
    void clearPhotoKey_whenVenueHadPhoto_thenClearsKeyAndKeepsStatus() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        existing.setPhotoObjectKey(OLD_KEY);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));

        venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(existing.getPhotoObjectKey()).isNull();
        assertThat(existing.getStatus()).isEqualTo(VenueStatus.ACTIVE);
        verify(venueRepository).save(existing);
    }

    @Test
    void clearPhotoKey_whenVenueHadNoPhoto_thenSavesNoopState() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));

        venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(existing.getPhotoObjectKey()).isNull();
        verify(venueRepository).save(existing);
    }

    @Test
    void clearPhotoKey_whenVenueNotFound_thenThrows() {
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID))
                .isInstanceOf(VenueNotFoundException.class);
    }

    @Test
    void clearPhotoKey_whenPendingHasStagedPhoto_thenDiscardsStagedPhoto() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        existing.setPhotoObjectKey(OLD_KEY);
        final VenuePendingUpdate pending =
                VenueTestFactory.createPendingUpdate(existing, VenueStatus.PENDING_UPDATE);
        pending.setPendingPhotoObjectKey(NEW_KEY);

        pending.applyContent(new VenueContent(
                "Bar Two",
                "ул. Кремлёвская, 1, Казань",
                VenueTestFactory.defaultContent().location(),
                VenueTestFactory.H3_RES_9,
                VenueCategory.FOOD,
                "Updated description",
                "Updated dish",
                "Updated music"
        ));

        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.of(pending));

        venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(existing.getPhotoObjectKey()).isNull();
        assertThat(pending.getPendingPhotoObjectKey()).isNull();
        verify(venuePendingUpdateRepository).save(pending);
    }

    @Test
    void clearPhotoKey_whenPendingExistsOnlyForPhoto_thenDeletesPending() {
        final Venue existing = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        existing.setPhotoObjectKey(OLD_KEY);
        final VenuePendingUpdate pending =
                VenuePendingUpdate.createForPhoto(existing, NEW_KEY);

        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(existing));
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.of(pending));

        venuePhotoUpdater.clearPhotoKey(VENUE_ID, OWNER_ID);

        assertThat(existing.getPhotoObjectKey()).isNull();
        verify(venuePendingUpdateRepository).delete(pending);
    }
}