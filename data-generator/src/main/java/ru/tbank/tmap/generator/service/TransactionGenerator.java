package ru.tbank.tmap.generator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.generator.config.GeneratorProperties;
import ru.tbank.tmap.generator.domain.Venue;
import ru.tbank.tmap.generator.kafka.event.TransactionEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TransactionGenerator {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private final VenueCache venueCache;
    private final GeneratorProperties generatorProps;

    public TransactionEvent generate() {
        Venue venue = venueCache.getRandomVenue();
        double[] shiftedCoordinates = shiftCoordinates(venue.getLat(), venue.getLng());

        double latitude = shiftedCoordinates[0];
        double longitude = shiftedCoordinates[1];

        return new TransactionEvent(
                UUID.randomUUID(),
                venue.getId(),
                randomAmount(),
                latitude,
                longitude,
                venue.getCategory(),
                randomOccurredAt()
        );
    }

    private double[] shiftCoordinates(double lat, double lng) {
        int spreadMeters = generatorProps.coordinateSpreadMeters();
        if (spreadMeters == 0) {
            return new double[]{lat, lng};
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * spreadMeters;

        double dLat = (distance * Math.cos(angle)) / METERS_PER_DEGREE_LAT;
        double dLng = (distance * Math.sin(angle)) / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));

        return new double[]{lat + dLat, lng + dLng};
    }

    private BigDecimal randomAmount() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double minAmount = generatorProps.amount().min().doubleValue();
        double maxAmount = generatorProps.amount().max().doubleValue();

        double amount = minAmount + random.nextDouble() * (maxAmount - minAmount);
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private Instant randomOccurredAt() {
        int maxDelay = generatorProps.maxOccurredAtDelaySeconds();
        if (maxDelay == 0) {
            return Instant.now();
        }
        int delaySeconds = ThreadLocalRandom.current().nextInt(maxDelay + 1);
        return Instant.now().minusSeconds(delaySeconds);
    }
}
