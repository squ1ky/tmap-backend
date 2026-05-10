package ru.tbank.tmap.heatmap.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryWriteRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;

class ClusterHistoryAggregatorTest {

    private static final Instant FROM = Instant.parse("2026-04-17T09:00:00Z");
    private static final Instant TO = Instant.parse("2026-04-17T11:00:00Z");

    @Mock
    private ClusterHistoryWriteRepository writeRepository;

    private ClusterHistoryAggregator aggregator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aggregator = new ClusterHistoryAggregator(writeRepository);
    }

    @Test
    void aggregate_whenInvoked_thenCallsRepositoryForEveryResolution() {
        given(writeRepository.refreshAggregates(any(), any(), any())).willReturn(0);

        aggregator.aggregate(FROM, TO);

        for (H3Resolution resolution : H3Resolution.values()) {
            verify(writeRepository).refreshAggregates(resolution, FROM, TO);
        }
        verifyNoMoreInteractions(writeRepository);
    }

    @Test
    void aggregate_whenAllResolutionsUpsertRows_thenReturnsTotalSum() {
        given(writeRepository.refreshAggregates(eq(H3Resolution.RES_7), any(), any())).willReturn(10);
        given(writeRepository.refreshAggregates(eq(H3Resolution.RES_8), any(), any())).willReturn(20);
        given(writeRepository.refreshAggregates(eq(H3Resolution.RES_9), any(), any())).willReturn(30);

        final int total = aggregator.aggregate(FROM, TO);

        assertThat(total).isEqualTo(60);
    }

    @Test
    void aggregate_whenRepositoryReturnsZero_thenReturnsZero() {
        given(writeRepository.refreshAggregates(any(), any(), any())).willReturn(0);

        final int total = aggregator.aggregate(FROM, TO);

        assertThat(total).isZero();
    }

    @Test
    void aggregate_whenRepositoryThrows_thenPropagatesException() {
        given(writeRepository.refreshAggregates(any(), any(), any()))
                .willThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> aggregator.aggregate(FROM, TO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }
}
