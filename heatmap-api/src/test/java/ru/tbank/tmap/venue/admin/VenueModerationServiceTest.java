package ru.tbank.tmap.venue.admin;

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
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.application.service.admin.AdminVenueService;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.exception.VenueModerationStateException;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class adminVenueServiceTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

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
        final Venue venue = pendingVenue();
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
        final Venue venue = pendingVenue();
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));
        given(venueRepository.save(venue)).willReturn(venue);

        final VenueDetails response =
                adminVenueService.rejectAdminVenue(VENUE_ID, "Address does not match coordinates");

        assertThat(venue.getStatus()).isEqualTo(VenueStatus.REJECTED);
        assertThat(venue.getRejectReason()).isEqualTo("Address does not match coordinates");
        assertThat(response.venue().getStatus()).isEqualTo(VenueStatus.REJECTED);
        assertThat(response.venue().getRejectReason()).isEqualTo("Address does not match coordinates");
        verify(venueRepository).save(venue);
    }

    @Test
    void verifyAdminVenue_whenVenueIsAlreadyActive_thenReturnConflict() {
        final Venue venue = pendingVenue();
        venue.setStatus(VenueStatus.ACTIVE);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));

        assertThatThrownBy(() -> adminVenueService.verifyAdminVenue(VENUE_ID))
                .isInstanceOf(VenueModerationStateException.class)
                .hasMessage("Only PENDING venues can be moderated");
    }

    @Test
    void verifyAdminVenue_whenVenueIsAlreadyRejected_thenReturnConflict() {
        final Venue venue = pendingVenue();
        venue.setStatus(VenueStatus.REJECTED);
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
    void rejectAdminVenue_whenReasonIsBlank_thenReturnValidationError() {
        assertThatThrownBy(() -> adminVenueService.rejectAdminVenue(VENUE_ID, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reject reason must not be blank");
    }

    @Test
    void verifyAdminVenue_whenPendingUpdateExists_thenApplyItAndDeleteDraft() {
        final Venue venue = pendingVenue();
        venue.setStatus(VenueStatus.ACTIVE);
        final VenuePendingUpdate pendingUpdate = pendingUpdate(venue, VenueStatus.PENDING_UPDATE, null);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.of(pendingUpdate));
        given(venueRepository.save(venue)).willReturn(venue);

        final VenueDetails response = adminVenueService.verifyAdminVenue(VENUE_ID);

        assertThat(response.pendingUpdate()).isNull();
        assertThat(response.venue().getName()).isEqualTo("Bar Two");
        assertThat(response.venue().getStatus()).isEqualTo(VenueStatus.ACTIVE);
        verify(venuePendingUpdateRepository).deleteById(pendingUpdate.getVenueId());
    }

    @Test
    void rejectAdminVenue_whenPendingUpdateExists_thenRejectDraftAndKeepPublishedVenueUntouched() {
        final Venue venue = pendingVenue();
        venue.setStatus(VenueStatus.ACTIVE);
        final VenuePendingUpdate pendingUpdate = pendingUpdate(venue, VenueStatus.PENDING_UPDATE, null);
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.of(pendingUpdate));
        given(venuePendingUpdateRepository.save(pendingUpdate)).willReturn(pendingUpdate);

        final VenueDetails response =
                adminVenueService.rejectAdminVenue(VENUE_ID, "Name does not match");

        assertThat(response.venue().getName()).isEqualTo("Bar One");
        assertThat(response.pendingUpdate()).isNotNull();
        assertThat(response.pendingUpdate().getStatus()).isEqualTo(VenueStatus.REJECTED);
        assertThat(response.pendingUpdate().getRejectReason()).isEqualTo("Name does not match");
    }

    private Venue pendingVenue() {
        return Venue.builder()
                .id(VENUE_ID)
                .ownerId(OWNER_ID)
                .name("Bar one")
                .address("Kazan Center, 2")
                .location(GeoPoint.of(55.7905, 49.1140))
                .h3Res9(617422037122678783L)
                .category(VenueCategory.ENTERTAINMENT)
                .build();
    }

    private VenuePendingUpdate pendingUpdate(
            final Venue venue,
            final VenueStatus status,
            final String rejectReason
    ) {
        final VenuePendingUpdate pendingUpdate = new VenuePendingUpdate(venue);
        pendingUpdate.setName("Bar Two");
        pendingUpdate.setAddress("Kazan Center, 5");
        pendingUpdate.setLocation(GeoPoint.of(55.8000, 49.1300));
        pendingUpdate.setH3Res9(617422037122678784L);
        pendingUpdate.setCategory(VenueCategory.FOOD);
        pendingUpdate.setDescription("Updated description");
        pendingUpdate.setDishOfDay("Soup");
        pendingUpdate.setMusic("Jazz");
        pendingUpdate.setStatus(status);
        pendingUpdate.setRejectReason(rejectReason);
        return pendingUpdate;
    }
}
