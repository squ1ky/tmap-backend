package ru.tbank.tmap.venue.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
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
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRepository;
import ru.tbank.tmap.user.domain.UserRole;
import ru.tbank.tmap.venue.application.VenueDetails;
import ru.tbank.tmap.venue.application.VenueH3Resolver;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class BusinessVenueServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final long H3_RES_9 = 617422037122678783L;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VenueH3Resolver venueH3Resolver;

    @Mock
    private VenuePendingUpdateRepository venuePendingUpdateRepository;

    private BusinessVenueService businessVenueService;

    @BeforeEach
    void setUp() {
        businessVenueService = new BusinessVenueService(
                venueRepository,
                userRepository,
                venueH3Resolver,
                venuePendingUpdateRepository
        );
    }

    @Test
    void getMyVenues_whenOwnerHasVenuesWithDifferentStatuses_thenReturnsAllOwnedVenues() {
        final List<Venue> venues = List.of(
                venue(VenueStatus.PENDING, null),
                venue(VenueStatus.ACTIVE, null),
                venue(VenueStatus.REJECTED, "Address does not match coordinates"),
                venue(VenueStatus.PENDING_UPDATE, null)
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
        final Venue rejectedVenue = venue(VenueStatus.REJECTED, "Address does not match coordinates");
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(rejectedVenue));
        given(venuePendingUpdateRepository.findByVenueId(VENUE_ID)).willReturn(Optional.empty());

        final Optional<VenueDetails> result = businessVenueService.getMyVenueById(OWNER_ID, VENUE_ID);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().venue()).isEqualTo(rejectedVenue);
        assertThat(result.orElseThrow().venue().getStatus()).isEqualTo(VenueStatus.REJECTED);
    }

    @Test
    void createVenue_whenRequestIsValid_thenCreatesPendingVenueForCurrentUser() {
        final User owner = owner();
        final VenueCreateCommand command = new VenueCreateCommand(
                "Cafe Pending",
                "Kazan Center, 1",
                GeoPoint.of(55.7905, 49.1140),
                VenueCategory.FOOD,
                "Fresh coffee",
                null,
                null
        );
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner));
        given(venueH3Resolver.toH3Res9(GeoPoint.of(55.7905, 49.1140))).willReturn(H3_RES_9);
        given(venueRepository.save(org.mockito.ArgumentMatchers.any(Venue.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        final VenueDetails result = businessVenueService.createVenue(OWNER_ID, command);

        assertThat(result.venue().getOwner()).isEqualTo(owner);
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.PENDING);
        assertThat(result.venue().getName()).isEqualTo("Cafe Pending");
        assertThat(result.venue().getH3Res9()).isEqualTo(H3_RES_9);
        assertThat(result.venue().getDescription()).isEqualTo("Fresh coffee");
        assertThat(result.pendingUpdate()).isNull();
    }

    @Test
    void updateVenue_whenVenueIsActive_thenStoresPendingUpdateAndKeepsPublishedData() {
        final Venue venue = venue(VenueStatus.ACTIVE, null);
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

        assertThat(result.venue().getName()).isEqualTo("Bar One");
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.ACTIVE);
        assertThat(result.pendingUpdate()).isNotNull();
        assertThat(result.pendingUpdate().getName()).isEqualTo("Bar Two");
        assertThat(result.pendingUpdate().getCategory()).isEqualTo(VenueCategory.FOOD);
        assertThat(result.pendingUpdate().getStatus()).isEqualTo(VenueStatus.PENDING_UPDATE);
    }

    @Test
    void updateVenue_whenVenueIsPending_thenUpdatesPublishedVenueInPlace() {
        final Venue venue = venue(VenueStatus.PENDING, null);
        final VenueUpdateCommand command = new VenueUpdateCommand(
                "Bar Pending Updated",
                "Kazan Center, 3",
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
        assertThat(result.venue().getName()).isEqualTo("Bar Pending Updated");
        assertThat(result.venue().getAddress()).isEqualTo("Kazan Center, 3");
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.PENDING);
    }

    @Test
    void updateVenue_whenVenueIsRejected_thenResubmitsUpdatedVenueForModeration() {
        final Venue venue = venue(VenueStatus.REJECTED, "Old reason");
        final VenueUpdateCommand command = new VenueUpdateCommand(
                "Bar One",
                "Kazan Center, 5",
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
        assertThat(result.venue().getAddress()).isEqualTo("Kazan Center, 5");
        assertThat(result.venue().getStatus()).isEqualTo(VenueStatus.PENDING);
        assertThat(result.venue().getRejectReason()).isNull();
    }

    private User owner() {
        return new User(OWNER_ID, OWNER_EMAIL, "password-hash", "Owner", UserRole.BUSINESS_OWNER);
    }

    private Venue venue(final VenueStatus status, final String rejectReason) {
        final Venue venue = new Venue(
                VENUE_ID,
                owner(),
                "Bar One",
                "Kazan Center, 2",
                GeoPoint.of(55.7905, 49.1140),
                H3_RES_9,
                VenueCategory.ENTERTAINMENT
        );
        venue.setStatus(status);
        venue.setRejectReason(rejectReason);
        return venue;
    }
}
