package ru.tbank.tmap.generator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.generator.TestFactory;
import ru.tbank.tmap.generator.config.GeneratorProperties;
import ru.tbank.tmap.generator.domain.Venue;
import ru.tbank.tmap.generator.kafka.event.TransactionEvent;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionGeneratorTest {

    @Mock
    private VenueCache venueCache;

    private TransactionGenerator transactionGenerator;

    private static final Venue TEST_VENUE = TestFactory.activeVenue(55.7558, 37.6173, "FOOD");
    private static final GeneratorProperties GENERATOR_PROPS = TestFactory.generatorProps();

    @BeforeEach
    void setUp() {
        transactionGenerator = new TransactionGenerator(venueCache, GENERATOR_PROPS);
    }

    @Test
    void generate_whenVenueExists_thenEventContainsVenueData() {
        when(venueCache.getRandomVenue()).thenReturn(TEST_VENUE);

        TransactionEvent event = transactionGenerator.generate();

        assertThat(event.transactionId()).isNotNull();
        assertThat(event.venueId()).isEqualTo(TEST_VENUE.getId());
        assertThat(event.category()).isEqualTo("FOOD");
    }

    @RepeatedTest(100)
    void generate_whenCalled_thenAmountIsWithinConfiguredRange() {
        when(venueCache.getRandomVenue()).thenReturn(TEST_VENUE);

        TransactionEvent event = transactionGenerator.generate();

        assertThat(event.amount())
                .isGreaterThanOrEqualTo(new BigDecimal("100.00"))
                .isLessThanOrEqualTo(new BigDecimal("5000.00"));
        assertThat(event.amount().scale()).isEqualTo(2);
    }

    @RepeatedTest(100)
    void generate_whenSpreadConfigured_thenCoordinatesAreWithinRadius() {
        when(venueCache.getRandomVenue()).thenReturn(TEST_VENUE);

        TransactionEvent event = transactionGenerator.generate();

        double distanceMeters = haversineMeters(
                TEST_VENUE.getLat(), TEST_VENUE.getLng(),
                event.lat(), event.lng()
        );
        assertThat(distanceMeters).isLessThanOrEqualTo(55.0);
    }

    @RepeatedTest(100)
    void generate_whenCalled_thenOccurredAtIsWithinDelay() {
        when(venueCache.getRandomVenue()).thenReturn(TEST_VENUE);

        Instant before = Instant.now().minusSeconds(11);
        TransactionEvent event = transactionGenerator.generate();

        assertThat(event.occurredAt())
                .isAfter(before)
                .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void generate_whenSpreadIsZero_thenCoordinatesMatchVenue() {
        int coordinateSpreadMeters = 0;
        int maxOccurredAtDelaySeconds = 10;

        GeneratorProperties noSpreadProps = TestFactory.generatorProps(
                coordinateSpreadMeters,
                maxOccurredAtDelaySeconds
        );
        TransactionGenerator noSpreadGenerator = new TransactionGenerator(venueCache, noSpreadProps);
        when(venueCache.getRandomVenue()).thenReturn(TEST_VENUE);

        TransactionEvent event = noSpreadGenerator.generate();

        assertThat(event.lat()).isEqualTo(TEST_VENUE.getLat());
        assertThat(event.lng()).isEqualTo(TEST_VENUE.getLng());
    }

    @Test
    void generate_whenDelayIsZero_thenOccurredAtIsCurrentTime() {
        int coordinateSpreadMeters = 50;
        int maxOccurredAtDelaySeconds = 0;

        GeneratorProperties noDelayProps = TestFactory.generatorProps(
                coordinateSpreadMeters,
                maxOccurredAtDelaySeconds
        );
        TransactionGenerator noDelayGenerator = new TransactionGenerator(venueCache, noDelayProps);
        when(venueCache.getRandomVenue()).thenReturn(TEST_VENUE);

        Instant before = Instant.now();
        TransactionEvent event = noDelayGenerator.generate();

        assertThat(event.occurredAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(Instant.now());
    }

    /**
     * The distance between two point on the Earth's surface in meters.
     * Haversine formula:
     * <a href="https://en.wikipedia.org/wiki/Haversine_formula">Wikipedia</a>
     */
    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
