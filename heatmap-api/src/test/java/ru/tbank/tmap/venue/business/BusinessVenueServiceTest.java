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
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.user.UserRole;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueStatus;
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
    private H3IndexService h3IndexService;

    private BusinessVenueService businessVenueService;

    @BeforeEach
    void setUp() {
        businessVenueService = new BusinessVenueService(
                venueRepository,
                userRepository,
                h3IndexService,
                new BusinessVenueMapper()
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

        final List<Venue> result = businessVenueService.getMyVenues(OWNER_ID);

        assertThat(result)
                .extracting(Venue::getStatus)
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

        final Optional<Venue> result = businessVenueService.getMyVenueById(OWNER_ID, VENUE_ID);

        assertThat(result).contains(rejectedVenue);
        assertThat(result.orElseThrow().getStatus()).isEqualTo(VenueStatus.REJECTED);
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
        given(h3IndexService.toH3(55.7905, 49.1140, H3Resolution.RES_9)).willReturn(H3_RES_9);
        given(venueRepository.save(org.mockito.ArgumentMatchers.any(Venue.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        final Venue result = businessVenueService.createVenue(OWNER_ID, command);

        assertThat(result.getOwner()).isEqualTo(owner);
        assertThat(result.getStatus()).isEqualTo(VenueStatus.PENDING);
        assertThat(result.getName()).isEqualTo("Cafe Pending");
        assertThat(result.getH3Res9()).isEqualTo(H3_RES_9);
        assertThat(result.getDescription()).isEqualTo("Fresh coffee");
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
