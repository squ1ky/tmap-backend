package ru.tbank.tmap.venue.application.service.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.venue.application.command.VenueCreateCommand;
import ru.tbank.tmap.venue.application.command.VenueUpdateCommand;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.application.service.VenueH3Resolver;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.api.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.VenueTestFactory;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class BusinessVenueServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final long H3_RES_9 = 617422037122678783L;

    @Mock
    private UserAccountFacade userAccountFacade;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenuePendingUpdateRepository venuePendingUpdateRepository;

    @Mock
    private VenueH3Resolver venueH3Resolver;

    private BusinessVenueService businessVenueService;

    @BeforeEach
    void setUp() {
        businessVenueService = new BusinessVenueService(
                userAccountFacade,
                venueRepository,
                venuePendingUpdateRepository,
                venueH3Resolver
        );
    }

    @Test
    void getMyVenues_whenOwnerHasVenuesWithDifferentStatuses_thenReturnsAllOwnedVenues() {
        final List<Venue> venues = List.of(
                VenueTestFactory.createVenue(VenueStatus.PENDING, null),
                VenueTestFactory.createVenue(VenueStatus.ACTIVE, null),
                VenueTestFactory.createVenue(VenueStatus.REJECTED, "Address does not match coordinates"),
                VenueTestFactory.createVenue(VenueStatus.PENDING_UPDATE, null)
        );
        given(venueRepository.findByOwnerIdOrderByNameAscIdAsc(OWNER_ID)).willReturn(venues);
        given(venuePendingUpdateRepository.findByVenueIdIn(List.of(VENUE_ID, VENUE_ID, VENUE_ID, VENUE_ID)))
                .willReturn(List.of());

        final List<VenueDetails> result = businessVenueService.getMyVenues(OWNER_ID);

        assertThat(result)
                .extracting(details -> details.venue().getStatus())
                .containsExactly(
                        VenueStatus.PENDING,
                        VenueStatus.ACTIVE,
                        VenueStatus.REJECTED,
                        VenueStatus.PENDING_UPDATE
                );
        verify(venueRepository).findByOwnerIdOrderByNameAscIdAsc(OWNER_ID);
    }

    @Test
    void getMyVenueById_whenVenueBelongsToOwner_thenReturnsVenueRegardlessOfStatus() {
        final Venue rejectedVenue = VenueTestFactory.createVenue(
                VenueStatus.REJECTED,
                "Address does not match coordinates"
        );
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(rejectedVenue));
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());

        final Optional<VenueDetails> result = businessVenueService.getMyVenueById(OWNER_ID, VENUE_ID);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().venue()).isEqualTo(rejectedVenue);
        assertThat(result.orElseThrow().venue().getStatus()).isEqualTo(VenueStatus.REJECTED);
    }

    @Test
    void createVenue_whenRequestIsValid_thenCreatesPendingVenueForCurrentUser() {
        String venueName = "Cafe Pending";
        String venueDescription = "Fresh coffee";

        final VenueCreateCommand command = new VenueCreateCommand(
                venueName,
                "Kazan Center, 1",
                GeoPoint.of(55.7905, 49.1140),
                VenueCategory.FOOD,
                venueDescription,
                null,
                null
        );

        given(userAccountFacade.findById(OWNER_ID)).willReturn(Optional.of(mock(UserView.class)));
        given(venueH3Resolver.toH3Res9(GeoPoint.of(55.7905, 49.1140))).willReturn(H3_RES_9);
        given(venueRepository.save(org.mockito.ArgumentMatchers.any(Venue.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        final VenueDetails result = businessVenueService.createVenue(OWNER_ID, command);

        assertThat(result.venue().getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.PENDING);
        assertThat(result.venue().getContent().name()).isEqualTo(venueName);
        assertThat(result.venue().getContent().h3Res9()).isEqualTo(H3_RES_9);
        assertThat(result.venue().getContent().description()).isEqualTo(venueDescription);
        assertThat(result.pendingUpdate()).isNull();
        verify(userAccountFacade, never()).promoteToBusinessOwner(OWNER_ID);
    }

    @Test
    void updateVenue_whenVenueIsActive_thenStoresPendingUpdateAndKeepsPublishedData() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        final VenueUpdateCommand command = new VenueUpdateCommand(
                "Bar Two",
                "Kazan Center, 5",
                GeoPoint.of(55.7920, 49.1220),
                VenueCategory.FOOD,
                "Updated description",
                "Soup",
                "Jazz"
        );
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(venue));
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());
        given(venueH3Resolver.resolveUpdatedH3Res9(venue, GeoPoint.of(55.7920, 49.1220)))
                .willReturn(617422037122678784L);
        given(venuePendingUpdateRepository.save(org.mockito.ArgumentMatchers.any(VenuePendingUpdate.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        final VenueDetails result = businessVenueService.updateVenue(OWNER_ID, VENUE_ID, command);

        assertThat(result.venue().getContent().name()).isEqualTo("Bar One");
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.ACTIVE);
        assertThat(result.pendingUpdate()).isNotNull();
        assertThat(result.pendingUpdate().getContent().name()).isEqualTo("Bar Two");
        assertThat(result.pendingUpdate().getContent().category()).isEqualTo(VenueCategory.FOOD);
        assertThat(result.pendingUpdate().getStatus()).isEqualTo(VenueStatus.PENDING_UPDATE);
    }

    @Test
    void updateVenue_whenVenueIsPending_thenUpdatesPublishedVenueInPlace() {
        final String venueName = "Bar Pending Updated";
        final String venueAddress = "Kazan Center, 3";
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.PENDING, null);
        final VenueUpdateCommand command = new VenueUpdateCommand(
                venueName,
                venueAddress,
                GeoPoint.of(55.7905, 49.1140),
                VenueCategory.ENTERTAINMENT,
                "Fresh draft",
                "Soup",
                "Jazz"
        );
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(venue));
        given(venueRepository.save(venue)).willReturn(venue);

        final VenueDetails result = businessVenueService.updateVenue(OWNER_ID, VENUE_ID, command);

        assertThat(result.pendingUpdate()).isNull();
        assertThat(result.venue().getContent().name()).isEqualTo(venueName);
        assertThat(result.venue().getContent().address()).isEqualTo(venueAddress);
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.PENDING);
    }

    @Test
    void updateVenue_whenVenueIsRejected_thenResubmitsUpdatedVenueForModeration() {
        final String venueAddress = "Kazan Center, 5";
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.REJECTED, "Old reason");
        final VenueUpdateCommand command = new VenueUpdateCommand(
                "Bar One",
                venueAddress,
                GeoPoint.of(55.7905, 49.1140),
                VenueCategory.ENTERTAINMENT,
                "Updated description",
                "Soup",
                "Jazz"
        );
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(venue));
        given(venueRepository.save(venue)).willReturn(venue);

        final VenueDetails result = businessVenueService.updateVenue(OWNER_ID, VENUE_ID, command);

        assertThat(result.pendingUpdate()).isNull();
        assertThat(result.venue().getContent().address()).isEqualTo(venueAddress);
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.PENDING);
        assertThat(result.venue().getRejectReason()).isNull();
    }
}
