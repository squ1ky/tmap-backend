package ru.tbank.tmap.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openapitools.model.AdminModerationDecision;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRole;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.repository.VenueQueryRepository;
import ru.tbank.tmap.venue.repository.VenueRepository;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueQueryRepository venueQueryRepository;

    private VenueService venueService;

    @BeforeEach
    void setUp() {
        venueService = new VenueService(
                venueRepository,
                venueQueryRepository,
                new VenuePublicMapper()
        );
    }

    @Test
    void verifyAdminVenue_whenVenueIsPending_thenActivateVenue() {
        final Venue venue = pendingVenue();
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));
        given(venueRepository.save(venue)).willReturn(venue);

        final AdminVenueModerationResponse response = venueService.verifyAdminVenue(VENUE_ID);

        assertThat(venue.getStatus()).isEqualTo(VenueStatus.ACTIVE);
        assertThat(venue.getRejectReason()).isNull();
        assertThat(response.getModerationStatus()).isEqualTo(VenueModerationStatus.ACTIVE);
        verify(venueRepository).save(venue);
    }

    @Test
    void rejectAdminVenue_whenVenueIsPending_thenRejectVenueWithReason() {
        final Venue venue = pendingVenue();
        final AdminModerationDecision decision = new AdminModerationDecision()
                .reason("Address does not match coordinates");
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));
        given(venueRepository.save(venue)).willReturn(venue);

        final AdminVenueModerationResponse response = venueService.rejectAdminVenue(VENUE_ID, decision);

        assertThat(venue.getStatus()).isEqualTo(VenueStatus.REJECTED);
        assertThat(venue.getRejectReason()).isEqualTo("Address does not match coordinates");
        assertThat(response.getModerationStatus()).isEqualTo(VenueModerationStatus.REJECTED);
        assertThat(response.getRejectReason()).isEqualTo("Address does not match coordinates");
        verify(venueRepository).save(venue);
    }

    @Test
    void verifyAdminVenue_whenVenueIsAlreadyActive_thenReturnConflict() {
        final Venue venue = pendingVenue();
        venue.setStatus(VenueStatus.ACTIVE);
        given(venueRepository.findById(VENUE_ID)).willReturn(Optional.of(venue));

        assertThatThrownBy(() -> venueService.verifyAdminVenue(VENUE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void rejectAdminVenue_whenReasonIsBlank_thenReturnValidationError() {
        final AdminModerationDecision decision = new AdminModerationDecision().reason(" ");

        assertThatThrownBy(() -> venueService.rejectAdminVenue(VENUE_ID, decision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reject reason must not be blank");
    }

    private Venue pendingVenue() {
        final User owner = new User(
                OWNER_ID,
                "owner@example.com",
                "password-hash",
                "Owner",
                UserRole.BUSINESS_OWNER
        );
        return new Venue(
                VENUE_ID,
                owner,
                "Bar One",
                "Kazan Center, 2",
                GeoPoint.of(55.7905, 49.1140),
                617422037122678783L,
                VenueCategory.ENTERTAINMENT
        );
    }
}
