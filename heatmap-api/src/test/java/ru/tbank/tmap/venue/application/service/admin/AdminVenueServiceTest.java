package ru.tbank.tmap.venue.application.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.VenueTestFactory;
import ru.tbank.tmap.venue.domain.exception.VenueModerationStateException;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class AdminVenueServiceTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenuePendingUpdateRepository venuePendingUpdateRepository;

    private AdminVenueService adminVenueService;

    @BeforeEach
    void setUp() {
        adminVenueService = new AdminVenueService(
                venueRepository,
                venuePendingUpdateRepository
        );
    }

    @Test
    void verifyAdminVenue_whenVenueIsPending_thenActivateVenue() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.PENDING, null);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));
        given(venueRepository.save(venue)).willReturn(venue);

        final VenueDetails response = adminVenueService.verifyAdminVenue(VENUE_ID);

        assertThat(venue.getStatus()).isEqualTo(VenueStatus.ACTIVE);
        assertThat(venue.getRejectReason()).isNull();
        assertThat(response.venue().getStatus()).isEqualTo(VenueStatus.ACTIVE);
        verify(venueRepository).save(venue);
    }

    @Test
    void rejectAdminVenue_whenVenueIsPending_thenRejectVenueWithReason() {
        final String rejectReason = "Address does not match coordinates";
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.PENDING, rejectReason);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));
        given(venueRepository.save(venue)).willReturn(venue);

        final VenueDetails response =
                adminVenueService.rejectAdminVenue(VENUE_ID, rejectReason);

        assertThat(venue.getStatus()).isEqualTo(VenueStatus.REJECTED);
        assertThat(venue.getRejectReason()).isEqualTo(rejectReason);
        assertThat(response.venue().getStatus()).isEqualTo(VenueStatus.REJECTED);
        assertThat(response.venue().getRejectReason()).isEqualTo(rejectReason);
        verify(venueRepository).save(venue);
    }

    @Test
    void verifyAdminVenue_whenVenueIsAlreadyActive_thenReturnConflict() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));

        assertThatThrownBy(() -> adminVenueService.verifyAdminVenue(VENUE_ID))
                .isInstanceOf(VenueModerationStateException.class)
                .hasMessage("Only PENDING venues can be moderated");
    }

    @Test
    void verifyAdminVenue_whenVenueIsAlreadyRejected_thenReturnConflict() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.REJECTED, null);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));

        assertThatThrownBy(() -> adminVenueService.verifyAdminVenue(VENUE_ID))
                .isInstanceOf(VenueModerationStateException.class)
                .hasMessage("Only PENDING venues can be moderated");
    }

    @Test
    void verifyAdminVenue_whenVenueDoesNotExist_thenReturnNotFound() {
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminVenueService.verifyAdminVenue(VENUE_ID))
                .isInstanceOf(VenueNotFoundException.class)
                .hasMessage("Venue not found");
    }

    @Test
    void verifyAdminVenue_whenPendingUpdateExists_thenApplyItAndDeleteDraft() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        final VenuePendingUpdate pendingUpdate =
                VenueTestFactory.createPendingUpdate(venue, VenueStatus.PENDING_UPDATE);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.of(pendingUpdate));
        given(venueRepository.save(venue)).willReturn(venue);

        final VenueDetails response = adminVenueService.verifyAdminVenue(VENUE_ID);

        assertThat(response.pendingUpdate()).isNull();
        assertThat(response.venue().getContent().name()).isEqualTo("Bar Two");
        assertThat(response.venue().getStatus()).isEqualTo(VenueStatus.ACTIVE);
        verify(venuePendingUpdateRepository).deleteById(pendingUpdate.getVenueId());
    }

    @Test
    void rejectAdminVenue_whenPendingUpdateExists_thenRejectDraftAndKeepPublishedVenueUntouched() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        final VenuePendingUpdate pendingUpdate =
                VenueTestFactory.createPendingUpdate(venue, VenueStatus.PENDING_UPDATE);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.of(pendingUpdate));
        given(venuePendingUpdateRepository.save(pendingUpdate)).willReturn(pendingUpdate);

        final String rejectReason = "Name does not match";
        final VenueDetails response = adminVenueService.rejectAdminVenue(VENUE_ID, rejectReason);

        assertThat(response.venue().getContent().name()).isEqualTo("Bar One");
        assertThat(response.pendingUpdate()).isNotNull();
        assertThat(response.pendingUpdate().getStatus()).isEqualTo(VenueStatus.REJECTED);
        assertThat(response.pendingUpdate().getRejectReason()).isEqualTo(rejectReason);
    }
}
