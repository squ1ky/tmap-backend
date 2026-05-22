package ru.tbank.tmap.heatmap.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.heatmap.application.port.cache.HeatmapClusterReader;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryQueryRepository;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CachedHeatmapClusterReader implements HeatmapClusterReader {

    private static final H3Resolution PARENT_RESOLUTION = H3Resolution.RES_6;

    private static final int TTL_BOUNDARY_SHORT_WINDOW_MINUTES = 60;
    private static final int TTL_BOUNDARY_MEDIUM_WINDOW_MINUTES = 1440;

    private static final Duration TTL_SHORT_WINDOW = Duration.ofSeconds(60);
    private static final Duration TTL_MEDIUM_WINDOW = Duration.ofMinutes(5);
    private static final Duration TTL_LONG_WINDOW = Duration.ofMinutes(15);

    private final ClusterHistoryQueryRepository queryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final H3IndexService h3IndexService;

    @Override
    public List<HeatmapClusterAggregate> read(
            final List<Long> parents,
            final H3Resolution resolution,
            final int windowMinutes,
            final Instant from,
            final Instant currentHour
    ) {
        if (parents.isEmpty()) {
            return List.of();
        }

        final List<String> keys = parents.stream()
                .map(parent -> buildKey(parent, resolution, windowMinutes, currentHour))
                .toList();

        final List<Object> cached = safeMultiGet(keys);

        final Map<Long, List<HeatmapClusterAggregate>> hits = new HashMap<>();
        final List<Long> misses = new ArrayList<>();

        for (int i = 0; i < parents.size(); i++) {
            final Long parent = parents.get(i);
            final Object value = cached.get(i);
            if (value == null) {
                misses.add(parent);
            } else {
                hits.put(parent, castClusters(value));
            }
        }

        log.debug("Cache lookup: parents={}, hits={}, misses={}",
                parents.size(), hits.size(), misses.size());

        if (!misses.isEmpty()) {
            final Map<Long, List<HeatmapClusterAggregate>> computed =
                    loadAndGroupMisses(misses, resolution, from, currentHour);

            cacheComputed(computed, misses, resolution, windowMinutes, currentHour);
            hits.putAll(computed);
        }

        final List<HeatmapClusterAggregate> merged = new ArrayList<>();
        for (Long parent : parents) {
            final List<HeatmapClusterAggregate> tileClusters = hits.get(parent);
            if (tileClusters != null) {
                merged.addAll(tileClusters);
            }
        }

        return merged;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Map<Long, List<HeatmapClusterAggregate>> loadAndGroupMisses(
            final List<Long> missedParents,
            final H3Resolution resolution,
            final Instant from,
            final Instant currentHour
    ) {
        final List<HeatmapClusterAggregate> dbClusters = queryRepository.findClustersByParents(
                missedParents, resolution, from, currentHour
        );

        final Map<Long, List<HeatmapClusterAggregate>> grouped = new HashMap<>();
        for (Long parent : missedParents) {
            grouped.put(parent, new ArrayList<>());
        }
        for (HeatmapClusterAggregate cluster : dbClusters) {
            final long parent = h3IndexService.cellToParent(cluster.h3Index(), PARENT_RESOLUTION);
            grouped.computeIfAbsent(parent, k -> new ArrayList<>()).add(cluster);
        }

        return grouped;
    }

    private void cacheComputed(
            final Map<Long, List<HeatmapClusterAggregate>> computed,
            final List<Long> missedParents,
            final H3Resolution resolution,
            final int windowMinutes,
            final Instant currentHour
    ) {
        final Duration ttl = ttlFor(windowMinutes);
        for (Long parent : missedParents) {
            final List<HeatmapClusterAggregate> tileClusters = computed.getOrDefault(parent, List.of());
            final String key = buildKey(parent, resolution, windowMinutes, currentHour);
            try {
                redisTemplate.opsForValue().set(key, tileClusters, ttl);
            } catch (DataAccessException e) {
                log.warn("Redis SET failed for key {}, continuing without caching: {}", key, e.getMessage());
            }
        }
    }

    private List<Object> safeMultiGet(final List<String> keys) {
        try {
            final List<Object> result = redisTemplate.opsForValue().multiGet(keys);
            if (result == null) {
                return nullList(keys.size());
            }
            return result;
        } catch (DataAccessException e) {
            log.warn("Redis MGET failed, falling back to DB for all parents: {}", e.getMessage());
            return nullList(keys.size());
        }
    }

    private static List<Object> nullList(final int size) {
        final List<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(null);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<HeatmapClusterAggregate> castClusters(final Object value) {
        return (List<HeatmapClusterAggregate>) value;
    }

    private static String buildKey(
            final long parent,
            final H3Resolution resolution,
            final int windowMinutes,
            final Instant currentHour
    ) {
        return "heatmap:tile=" + Long.toHexString(parent)
                + ":res=" + resolution.getValue()
                + ":window=" + windowMinutes
                + ":hour=" + currentHour.toEpochMilli();
    }

    private static Duration ttlFor(final int windowMinutes) {
        if (windowMinutes <= TTL_BOUNDARY_SHORT_WINDOW_MINUTES) {
            return TTL_SHORT_WINDOW;
        }
        if (windowMinutes <= TTL_BOUNDARY_MEDIUM_WINDOW_MINUTES) {
            return TTL_MEDIUM_WINDOW;
        }
        return TTL_LONG_WINDOW;
    }
}
