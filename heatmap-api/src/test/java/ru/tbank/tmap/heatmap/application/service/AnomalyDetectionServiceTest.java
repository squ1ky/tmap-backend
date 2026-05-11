package ru.tbank.tmap.heatmap.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.tbank.tmap.heatmap.application.config.AnomalyDetectionProperties;
import ru.tbank.tmap.heatmap.domain.AnomalyDetectionRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;

class AnomalyDetectionServiceTest {

    private static final Instant HOUR_BUCKET = Instant.parse("2026-04-17T10:00:00Z");

    private static final double RATIO_THRESHOLD = 2.0;
    private static final int MIN_BASELINE = 10;
    private static final int MIN_BASELINE_DAYS = 3;

    @Mock
    private AnomalyDetectionRepository anomalyRepository;

    private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        final AnomalyDetectionProperties properties = new AnomalyDetectionProperties(
                RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
        );
        anomalyDetectionService = new AnomalyDetectionService(anomalyRepository, properties);
    }

    @Test
    void detectFor_whenInvoked_thenCallsRepositoryForEveryResolution() {
        given(anomalyRepository.recompute(any(), any(), anyDouble(), anyInt(), anyInt()))
                .willReturn(0);

        anomalyDetectionService.detectFor(HOUR_BUCKET);

        for (H3Resolution resolution : H3Resolution.values()) {
            verify(anomalyRepository).recompute(
                    resolution, HOUR_BUCKET,
                    RATIO_THRESHOLD, MIN_BASELINE, MIN_BASELINE_DAYS
            );
        }
        verifyNoMoreInteractions(anomalyRepository);
    }

    @Test
    void detectFor_whenInvoked_thenPassesPropertiesFromConfig() {
        given(anomalyRepository.recompute(any(), any(), anyDouble(), anyInt(), anyInt()))
                .willReturn(0);

        anomalyDetectionService.detectFor(HOUR_BUCKET);

        verify(anomalyRepository, times(H3Resolution.values().length))
                .recompute(any(), eq(HOUR_BUCKET),
                        eq(RATIO_THRESHOLD), eq(MIN_BASELINE), eq(MIN_BASELINE_DAYS));
    }

    @Test
    void detectFor_whenAllResolutionsReturnAnomalies_thenReturnsTotalSum() {
        given(anomalyRepository.recompute(eq(H3Resolution.RES_7), any(), anyDouble(), anyInt(), anyInt()))
                .willReturn(2);
        given(anomalyRepository.recompute(eq(H3Resolution.RES_8), any(), anyDouble(), anyInt(), anyInt()))
                .willReturn(5);
        given(anomalyRepository.recompute(eq(H3Resolution.RES_9), any(), anyDouble(), anyInt(), anyInt()))
                .willReturn(3);

        final int total = anomalyDetectionService.detectFor(HOUR_BUCKET);

        assertThat(total).isEqualTo(10);
    }

    @Test
    void detectFor_whenNoAnomaliesFound_thenReturnsZero() {
        given(anomalyRepository.recompute(any(), any(), anyDouble(), anyInt(), anyInt()))
                .willReturn(0);

        final int total = anomalyDetectionService.detectFor(HOUR_BUCKET);

        assertThat(total).isZero();
    }

    @Test
    void detectFor_whenRepositoryThrows_thenPropagatesException() {
        given(anomalyRepository.recompute(any(), any(), anyDouble(), anyInt(), anyInt()))
                .willThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> anomalyDetectionService.detectFor(HOUR_BUCKET))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }
}
