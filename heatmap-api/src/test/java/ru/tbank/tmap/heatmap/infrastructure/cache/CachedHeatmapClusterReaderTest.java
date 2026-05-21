package ru.tbank.tmap.heatmap.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryQueryRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;

class CachedHeatmapClusterReaderTest {

    private static final H3Resolution RESOLUTION = H3Resolution.RES_8;
    private static final int WINDOW_MINUTES = 60;
    private static final Instant FROM = Instant.parse("2026-04-17T09:20:00Z");
    private static final Instant CURRENT_HOUR = Instant.parse("2026-04-17T10:00:00Z");

    private static final long PARENT_A = Long.parseUnsignedLong("86115b227ffffff", 16);
    private static final long PARENT_B = Long.parseUnsignedLong("86115b22fffffff", 16);

    private static final long CLUSTER_IN_A = Long.parseUnsignedLong("88115b22b1fffff", 16);
    private static final long CLUSTER_IN_B = Long.parseUnsignedLong("88115b22e1fffff", 16);

    private static final String KEY_A =
            "heatmap:tile=" + Long.toHexString(PARENT_A) + ":res=8:window=60:hour="
                    + CURRENT_HOUR.toEpochMilli();
    private static final String KEY_B =
            "heatmap:tile=" + Long.toHexString(PARENT_B) + ":res=8:window=60:hour="
                    + CURRENT_HOUR.toEpochMilli();

    @Mock
    private ClusterHistoryQueryRepository queryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private H3IndexService h3IndexService;

    private CachedHeatmapClusterReader reader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        reader = new CachedHeatmapClusterReader(queryRepository, redisTemplate, h3IndexService);
    }

    @Test
    void read_whenAllTilesCached_thenRepoNotCalled() {
        final HeatmapClusterAggregate clusterA = clusterAt(CLUSTER_IN_A);
        final HeatmapClusterAggregate clusterB = clusterAt(CLUSTER_IN_B);
        given(valueOperations.multiGet(List.of(KEY_A, KEY_B)))
                .willReturn(List.of(List.of(clusterA), List.of(clusterB)));

        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(PARENT_A, PARENT_B), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).extracting(HeatmapClusterAggregate::h3Index)
                .containsExactly(CLUSTER_IN_A, CLUSTER_IN_B);
        verify(queryRepository, never()).findClustersByParents(anyList(), any(), any(), any());
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void read_whenAllTilesMissed_thenLoadsFromRepoAndSetsAllKeys() {
        given(valueOperations.multiGet(List.of(KEY_A, KEY_B)))
                .willReturn(Arrays.asList(null, null));

        final HeatmapClusterAggregate clusterA = clusterAt(CLUSTER_IN_A);
        final HeatmapClusterAggregate clusterB = clusterAt(CLUSTER_IN_B);
        given(queryRepository.findClustersByParents(
                List.of(PARENT_A, PARENT_B), RESOLUTION, FROM, CURRENT_HOUR
        )).willReturn(List.of(clusterA, clusterB));
        given(h3IndexService.cellToParent(CLUSTER_IN_A, H3Resolution.RES_6)).willReturn(PARENT_A);
        given(h3IndexService.cellToParent(CLUSTER_IN_B, H3Resolution.RES_6)).willReturn(PARENT_B);

        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(PARENT_A, PARENT_B), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).extracting(HeatmapClusterAggregate::h3Index)
                .containsExactly(CLUSTER_IN_A, CLUSTER_IN_B);
        verify(valueOperations).set(eq(KEY_A), eq(List.of(clusterA)), eq(Duration.ofSeconds(60)));
        verify(valueOperations).set(eq(KEY_B), eq(List.of(clusterB)), eq(Duration.ofSeconds(60)));
    }

    @Test
    void read_whenPartialHit_thenRepoCalledOnlyForMissedParents() {
        final HeatmapClusterAggregate clusterA = clusterAt(CLUSTER_IN_A);
        final HeatmapClusterAggregate clusterB = clusterAt(CLUSTER_IN_B);
        given(valueOperations.multiGet(List.of(KEY_A, KEY_B)))
                .willReturn(Arrays.asList(List.of(clusterA), null));

        given(queryRepository.findClustersByParents(
                List.of(PARENT_B), RESOLUTION, FROM, CURRENT_HOUR
        )).willReturn(List.of(clusterB));
        given(h3IndexService.cellToParent(CLUSTER_IN_B, H3Resolution.RES_6)).willReturn(PARENT_B);

        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(PARENT_A, PARENT_B), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).extracting(HeatmapClusterAggregate::h3Index)
                .containsExactly(CLUSTER_IN_A, CLUSTER_IN_B);
        verify(queryRepository, times(1)).findClustersByParents(
                List.of(PARENT_B), RESOLUTION, FROM, CURRENT_HOUR
        );
        verify(valueOperations, never()).set(eq(KEY_A), any(), any(Duration.class));
        verify(valueOperations).set(eq(KEY_B), eq(List.of(clusterB)), eq(Duration.ofSeconds(60)));
    }

    @Test
    void read_whenEmptyParents_thenReturnsEmptyAndDoesNotTouchRedisOrRepo() {
        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).isEmpty();
        verify(valueOperations, never()).multiGet(anyList());
        verify(queryRepository, never()).findClustersByParents(anyList(), any(), any(), any());
    }

    @Test
    void read_whenRedisMgetFails_thenFallsBackToRepo() {
        given(valueOperations.multiGet(List.of(KEY_A, KEY_B)))
                .willThrow(new RedisConnectionFailureException("boom"));

        final HeatmapClusterAggregate clusterA = clusterAt(CLUSTER_IN_A);
        given(queryRepository.findClustersByParents(
                List.of(PARENT_A, PARENT_B), RESOLUTION, FROM, CURRENT_HOUR
        )).willReturn(List.of(clusterA));
        given(h3IndexService.cellToParent(CLUSTER_IN_A, H3Resolution.RES_6)).willReturn(PARENT_A);

        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(PARENT_A, PARENT_B), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).extracting(HeatmapClusterAggregate::h3Index)
                .containsExactly(CLUSTER_IN_A);
    }

    @Test
    void read_whenRedisSetFails_thenStillReturnsResultFromRepo() {
        given(valueOperations.multiGet(List.of(KEY_A, KEY_B)))
                .willReturn(Arrays.asList(null, null));

        final HeatmapClusterAggregate clusterA = clusterAt(CLUSTER_IN_A);
        final HeatmapClusterAggregate clusterB = clusterAt(CLUSTER_IN_B);
        given(queryRepository.findClustersByParents(
                List.of(PARENT_A, PARENT_B), RESOLUTION, FROM, CURRENT_HOUR
        )).willReturn(List.of(clusterA, clusterB));
        given(h3IndexService.cellToParent(CLUSTER_IN_A, H3Resolution.RES_6)).willReturn(PARENT_A);
        given(h3IndexService.cellToParent(CLUSTER_IN_B, H3Resolution.RES_6)).willReturn(PARENT_B);

        willThrow(new RedisConnectionFailureException("boom"))
                .given(valueOperations).set(anyString(), any(), any(Duration.class));

        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(PARENT_A, PARENT_B), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).extracting(HeatmapClusterAggregate::h3Index)
                .containsExactly(CLUSTER_IN_A, CLUSTER_IN_B);
    }

    @Test
    void read_whenRepoReturnsNothingForParent_thenEmptyListIsCached() {
        given(valueOperations.multiGet(List.of(KEY_A)))
                .willReturn(Arrays.asList((Object) null));
        given(queryRepository.findClustersByParents(
                List.of(PARENT_A), RESOLUTION, FROM, CURRENT_HOUR
        )).willReturn(List.of());

        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(PARENT_A), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).isEmpty();
        verify(valueOperations).set(eq(KEY_A), eq(List.of()), eq(Duration.ofSeconds(60)));
    }

    @Test
    void read_whenWindowExceeds1440_thenLongerTtlApplied() {
        given(valueOperations.multiGet(List.of(KEY_A_WindowExceeds())))
                .willReturn(Arrays.asList((Object) null));
        given(queryRepository.findClustersByParents(
                List.of(PARENT_A), RESOLUTION, FROM, CURRENT_HOUR
        )).willReturn(List.of());

        reader.read(List.of(PARENT_A), RESOLUTION, 2000, FROM, CURRENT_HOUR);

        verify(valueOperations).set(eq(KEY_A_WindowExceeds()), eq(List.of()), eq(Duration.ofMinutes(15)));
    }

    private static String KEY_A_WindowExceeds() {
        return "heatmap:tile=" + Long.toHexString(PARENT_A) + ":res=8:window=2000:hour="
                + CURRENT_HOUR.toEpochMilli();
    }

    private static HeatmapClusterAggregate clusterAt(final long h3Index) {
        return new HeatmapClusterAggregate(
                h3Index,
                0.0,
                0.0,
                10,
                new BigDecimal("100.00"),
                new BigDecimal("1000.00"),
                Instant.parse("2026-04-17T10:15:00Z"),
                false,
                null
        );
    }
}
