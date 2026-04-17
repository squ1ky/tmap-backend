package ru.tbank.tmap.generator;

import ru.tbank.tmap.generator.config.GeneratorProperties;
import ru.tbank.tmap.generator.domain.Venue;
import ru.tbank.tmap.generator.kafka.event.TransactionEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TestFactory {

    private static final String VENUE_NAME = "Test Venue";
    private static final String VENUE_CATEGORY = "cafe";
    private static final String VENUE_STATUS_ACTIVE = "ACTIVE";
    private static final double VENUE_LAT = 55.75;
    private static final double VENUE_LNG = 37.61;
    private static final BigDecimal TRANSACTION_AMOUNT = new BigDecimal("500.00");

    private TestFactory() {
    }

    public static Venue activeVenue(double lat, double lng, String category) {
        return new Venue(
                UUID.randomUUID(),
                VENUE_NAME,
                lat,
                lng,
                category,
                VENUE_STATUS_ACTIVE
        );
    }

    public static Venue venueWithStatus(String name, String status) {
        return new Venue(
                UUID.randomUUID(),
                name,
                VENUE_LAT,
                VENUE_LNG,
                VENUE_CATEGORY,
                status
        );
    }

    public static TransactionEvent transactionEvent() {
        return new TransactionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TRANSACTION_AMOUNT,
                VENUE_LAT,
                VENUE_LNG,
                VENUE_CATEGORY,
                Instant.now()
        );
    }

    public static TransactionEvent transactionEvent(UUID venueId) {
        return new TransactionEvent(
                UUID.randomUUID(),
                venueId,
                TRANSACTION_AMOUNT,
                VENUE_LAT,
                VENUE_LNG,
                VENUE_CATEGORY,
                Instant.now()
        );
    }

    public static GeneratorProperties generatorProps() {
        return new GeneratorProperties(
                "transactions",
                new GeneratorProperties.Batch(5, 20, 2000, 5000),
                new GeneratorProperties.Amount(
                        new BigDecimal("100.00"),
                        new BigDecimal("5000.00")
                ),
                50,
                10
        );
    }

    public static GeneratorProperties generatorProps(int spreadMeters, int maxDelaySeconds) {
        GeneratorProperties base = generatorProps();
        return new GeneratorProperties(
                base.topic(),
                base.batch(),
                base.amount(),
                spreadMeters,
                maxDelaySeconds
        );
    }

    public static GeneratorProperties fixedBatchGeneratorProps(int batchSize) {
        return new GeneratorProperties(
                "transactions",
                new GeneratorProperties.Batch(batchSize, batchSize, 2000, 2000),
                new GeneratorProperties.Amount(
                        new BigDecimal("100.00"),
                        new BigDecimal("5000.00")
                ),
                50,
                10
        );
    }
}
