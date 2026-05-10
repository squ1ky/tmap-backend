package ru.tbank.tmap.heatmap.application.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import ru.tbank.tmap.heatmap.application.service.AnomalyDetector;
import ru.tbank.tmap.heatmap.application.service.ClusterHistoryAggregator;

class HeatmapSchedulerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-17T10:20:00Z");
    private static final Instant CURRENT_HOUR = Instant.parse("2026-04-17T10:00:00Z");
    private static final Instant WINDOW_FROM = Instant.parse("2026-04-17T09:00:00Z"); // currentHour - 1h
    private static final Instant WINDOW_TO = Instant.parse("2026-04-17T11:00:00Z"); // currentHour + 1h

    private static final int LOOKBACK_HOURS = 1;

    @Mock
    private ClusterHistoryAggregator historyAggregator;

    @Mock
    private AnomalyDetector anomalyDetector;

    private HeatmapScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new HeatmapScheduler(
                historyAggregator,
                anomalyDetector,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        ReflectionTestUtils.setField(scheduler, "lookbackHours", LOOKBACK_HOURS);
    }

    @Test
    void refresh_whenBothStepsSucceed_thenAggregatesThenDetectsAnomalies() {
        given(historyAggregator.aggregate(WINDOW_FROM, WINDOW_TO)).willReturn(10);
        given(anomalyDetector.detectFor(CURRENT_HOUR)).willReturn(3);

        scheduler.refresh();

        final InOrder inOrder = Mockito.inOrder(historyAggregator, anomalyDetector);
        inOrder.verify(historyAggregator).aggregate(WINDOW_FROM, WINDOW_TO);
        inOrder.verify(anomalyDetector).detectFor(CURRENT_HOUR);
    }

    @Test
    void refresh_whenAnomalyDetectorThrows_thenSwallowsExceptionAndCompletes() {
        given(historyAggregator.aggregate(WINDOW_FROM, WINDOW_TO)).willReturn(10);
        given(anomalyDetector.detectFor(CURRENT_HOUR))
                .willThrow(new RuntimeException("exception"));

        assertThatCode(() -> scheduler.refresh()).doesNotThrowAnyException();

        verify(historyAggregator).aggregate(WINDOW_FROM, WINDOW_TO);
        verify(anomalyDetector).detectFor(CURRENT_HOUR);
    }

    @Test
    void refresh_whenAggregatorThrows_thenAnomalyDetectorIsNotInvoked() {
        given(historyAggregator.aggregate(any(), any()))
                .willThrow(new RuntimeException("aggregation failed"));

        assertThatCode(() -> scheduler.refresh())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("aggregation failed");

        verify(historyAggregator).aggregate(WINDOW_FROM, WINDOW_TO);
        verifyNoInteractions(anomalyDetector);
    }

    @Test
    void refresh_whenInvokedMultipleTimes_thenComputesWindowFromCurrentClockEachTime() {
        given(historyAggregator.aggregate(any(), any())).willReturn(0);
        given(anomalyDetector.detectFor(any())).willReturn(0);

        scheduler.refresh();
        scheduler.refresh();

        verify(historyAggregator, times(2)).aggregate(WINDOW_FROM, WINDOW_TO);
        verify(anomalyDetector, times(2)).detectFor(CURRENT_HOUR);
    }

    @Test
    void refresh_whenLookbackHoursIsThree_thenWidensTheAggregationWindow() {
        ReflectionTestUtils.setField(scheduler, "lookbackHours", 3);
        final Instant widerFrom = Instant.parse("2026-04-17T07:00:00Z"); // currentHour - 3h

        given(historyAggregator.aggregate(widerFrom, WINDOW_TO)).willReturn(0);
        given(anomalyDetector.detectFor(CURRENT_HOUR)).willReturn(0);

        scheduler.refresh();

        verify(historyAggregator).aggregate(widerFrom, WINDOW_TO);
        verify(anomalyDetector).detectFor(CURRENT_HOUR);
        verify(anomalyDetector, never()).detectFor(WINDOW_FROM);
    }
}
