package ru.tbank.tmap.generator.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.generator")
@Validated
public record GeneratorProperties(
        @NotBlank String topic,
        @NotNull Batch batch,
        @NotNull Amount amount,
        @Min(0) int coordinateSpreadMeters,
        @Min(0) int maxOccurredAtDelaySeconds
) {
    public record Batch(
            @Min(1) int minSize,
            @Min(1) int maxSize,
            @Min(1) long minIntervalMs,
            @Min(1) long maxIntervalMs
    ) {
        public Batch {
            if (minSize > maxSize) {
                throw new IllegalArgumentException(
                        "batch.min-size (%d) > batch.max-size (%d)"
                                .formatted(minSize, maxSize));
            }
            if (minIntervalMs > maxIntervalMs) {
                throw new IllegalArgumentException(
                        "batch.min-interval-ms (%d) > batch.max-interval-ms (%d)"
                                .formatted(minIntervalMs, maxIntervalMs));
            }
        }
    }

    public record Amount(
            @NotNull BigDecimal min,
            @NotNull BigDecimal max
    ) {
        public Amount {
            if (min != null && max != null && min.compareTo(max) > 0) {
                throw new IllegalArgumentException(
                        "amount.min (%s) > amount.max (%s)"
                                .formatted(min, max));
            }
        }
    }
}
