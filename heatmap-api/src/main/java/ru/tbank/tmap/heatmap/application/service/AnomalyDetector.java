package ru.tbank.tmap.heatmap.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.heatmap.application.config.AnomalyDetectionProperties;
import ru.tbank.tmap.heatmap.domain.AnomalyDetectionRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetector {

    private final AnomalyDetectionRepository anomalyRepository;
    private final AnomalyDetectionProperties anomalyDetectionProperties;

    @Transactional
    public int detectFor(final Instant hourBucket) {
        int total = 0;
        for (H3Resolution resolution : H3Resolution.values()) {
            int detected = anomalyRepository.recompute(
                    resolution,
                    hourBucket,
                    anomalyDetectionProperties.ratioThreshold(),
                    anomalyDetectionProperties.minBaseline(),
                    anomalyDetectionProperties.minBaselineDays()
            );
            log.debug("Anomalies detected: resolution={}, hourBucket={}, count={}",
                    resolution, hourBucket, detected);
            total += detected;
        }
        return total;
    }
}
