package ru.tbank.tmap.heatmap.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anomaly")
public record AnomalyDetectionProperties(
        double ratioThreshold,
        int minBaseline,
        int minBaselineDays
) {
    public AnomalyDetectionProperties {
        if (ratioThreshold <= 1.0) {
            throw new IllegalArgumentException(
                    "ratioThreshold must be > 1.0, got " + ratioThreshold);
        }
        if (minBaseline < 1) {
            throw new IllegalArgumentException(
                    "minBaseline must be >= 1, got " + minBaseline);
        }
        if (minBaselineDays < 1 || minBaselineDays > 7) {
            throw new IllegalArgumentException(
                    "minBaselineDays must be in [1, 7], got " + minBaselineDays);
        }
    }
}
