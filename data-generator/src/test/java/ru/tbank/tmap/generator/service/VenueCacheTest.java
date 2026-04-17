package ru.tbank.tmap.generator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.generator.TestFactory;
import ru.tbank.tmap.generator.domain.Venue;
import ru.tbank.tmap.generator.repository.VenueRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VenueCacheTest {

    @Mock
    private VenueRepository venueRepository;

    private VenueCache venueCache;

    private final static String VENUE_CATEGORY_CAFE = "cafe";
    private final static String VENUE_CATEGORY_BAR = "bar";

    @BeforeEach
    void setUp() {
        venueCache = new VenueCache(venueRepository);
    }

    @Test
    void isEmpty_whenNoRefreshCalled_thenReturnsTrue() {
        assertThat(venueCache.isEmpty()).isTrue();
    }

    @Test
    void refresh_whenVenuesExist_thenCacheIsNotEmpty() {
        Venue venue = TestFactory.activeVenue(55.0, 37.0, VENUE_CATEGORY_CAFE);
        when(venueRepository.findAllActive()).thenReturn(List.of(venue));

        venueCache.refresh();

        assertThat(venueCache.isEmpty()).isFalse();
        assertThat(venueCache.getRandomVenue()).isEqualTo(venue);
    }

    @Test
    void refresh_whenCalledTwice_thenCacheContainsLatestData() {
        Venue firstVenue = TestFactory.activeVenue(55.0, 37.0, VENUE_CATEGORY_CAFE);
        Venue secondVenue = TestFactory.activeVenue(56.0, 38.0, VENUE_CATEGORY_BAR);

        when(venueRepository.findAllActive()).thenReturn(List.of(firstVenue));
        venueCache.refresh();

        when(venueRepository.findAllActive()).thenReturn(List.of(secondVenue));
        venueCache.refresh();

        assertThat(venueCache.getRandomVenue()).isEqualTo(secondVenue);
    }

    @Test
    void getRandomVenue_whenCacheIsEmpty_thenThrowsIllegalState() {
        assertThatThrownBy(() -> venueCache.getRandomVenue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active venues");
    }

    @Test
    void refresh_whenNoActiveVenues_thenCacheBecomesEmpty() {
        Venue venue = TestFactory.activeVenue(55.0, 37.0, VENUE_CATEGORY_CAFE);
        when(venueRepository.findAllActive()).thenReturn(List.of(venue));
        venueCache.refresh();

        when(venueRepository.findAllActive()).thenReturn(List.of());
        venueCache.refresh();

        assertThat(venueCache.isEmpty()).isTrue();
    }
}
