package ru.tbank.tmap.heatmap.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anomaly")
public record AnomalyDetectionProperties(
        double ratioThreshold,
        int minBaseline,
        int minBaselineDays
) {
    private static final double MIN_RATIO_THRESHOLD = 1.0;
    private static final int MIN_BASELINE_VALUE = 1;
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 7;

    public AnomalyDetectionProperties {
        if (ratioThreshold <= MIN_RATIO_THRESHOLD) {
            throw new IllegalArgumentException(
                    "ratioThreshold must be > " + MIN_RATIO_THRESHOLD + ", got " + ratioThreshold);
        }
        if (minBaseline < MIN_BASELINE_VALUE) {
            throw new IllegalArgumentException(
                    "minBaseline must be >= " + MIN_BASELINE_VALUE + ", got " + minBaseline);
        }
        if (minBaselineDays < MIN_DAYS || minBaselineDays > MAX_DAYS) {
            throw new IllegalArgumentException(
                    "minBaselineDays must be in [" + MIN_DAYS + ", " + MAX_DAYS + "], got " + minBaselineDays);
        }
    }
}
