package ru.tbank.tmap.heatmap.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anomaly")
public record AnomalyDetectionProperties(
        double ratioThreshold,
        int minBaseline,
        int minBaselineDays
) {
}
