package ru.tbank.tmap.heatmap.infrastructure.cache;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryQueryRepository;
import ru.tbank.tmap.infrastructure.redis.RedisConfig;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;

@DataRedisTest
@Import({
        TestcontainersConfiguration.class,
        RedisConfig.class,
        CachedHeatmapClusterReader.class
})
@TestPropertySource(properties = "app.cache.enabled=true")
class CachedHeatmapClusterReaderIntegrationTest {

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

    @MockitoBean
    private ClusterHistoryQueryRepository queryRepository;

    @MockitoBean
    private H3IndexService h3IndexService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CachedHeatmapClusterReader reader;

    @BeforeEach
    void cleanRedis() {
        redisTemplate.delete(List.of(KEY_A, KEY_B));
    }

    @Test
    void read_whenCacheIsEmpty_thenLoadsFromRepoAndPopulatesCache() {
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
        assertThat(redisTemplate.hasKey(KEY_A)).isTrue();
        assertThat(redisTemplate.hasKey(KEY_B)).isTrue();
    }

    @Test
    void read_whenAllInCache_thenRepoIsNotCalled() {
        final HeatmapClusterAggregate clusterA = clusterAt(CLUSTER_IN_A);
        final HeatmapClusterAggregate clusterB = clusterAt(CLUSTER_IN_B);

        redisTemplate.opsForValue().set(KEY_A, new ArrayList<>(List.of(clusterA)));
        redisTemplate.opsForValue().set(KEY_B, new ArrayList<>(List.of(clusterB)));

        final List<HeatmapClusterAggregate> result = reader.read(
                List.of(PARENT_A, PARENT_B), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(result).extracting(HeatmapClusterAggregate::h3Index)
                .containsExactly(CLUSTER_IN_A, CLUSTER_IN_B);
        verify(queryRepository, never()).findClustersByParents(anyList(), any(), any(), any());
    }

    @Test
    void read_whenSerializedClusterRoundtripsThroughRedis_thenFieldsAreIntact() {
        final HeatmapClusterAggregate cluster = new HeatmapClusterAggregate(
                CLUSTER_IN_A,
                55.796127,
                49.106414,
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z"),
                true,
                new BigDecimal("3.40")
        );
        given(queryRepository.findClustersByParents(
                List.of(PARENT_A), RESOLUTION, FROM, CURRENT_HOUR
        )).willReturn(List.of(cluster));
        given(h3IndexService.cellToParent(CLUSTER_IN_A, H3Resolution.RES_6)).willReturn(PARENT_A);

        reader.read(List.of(PARENT_A), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR);

        final List<HeatmapClusterAggregate> secondCall = reader.read(
                List.of(PARENT_A), RESOLUTION, WINDOW_MINUTES, FROM, CURRENT_HOUR
        );

        assertThat(secondCall).hasSize(1);
        final HeatmapClusterAggregate restored = secondCall.getFirst();
        assertThat(restored.h3Index()).isEqualTo(CLUSTER_IN_A);
        assertThat(restored.centerLat()).isEqualTo(55.796127);
        assertThat(restored.centerLng()).isEqualTo(49.106414);
        assertThat(restored.txCount()).isEqualTo(128);
        assertThat(restored.avgCheck()).isEqualByComparingTo("742.50");
        assertThat(restored.sumAmount()).isEqualByComparingTo("95040.00");
        assertThat(restored.updatedAt()).isEqualTo(Instant.parse("2026-04-17T10:15:00Z"));
        assertThat(restored.isAnomaly()).isTrue();
        assertThat(restored.anomalyRatio()).isEqualByComparingTo("3.40");
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