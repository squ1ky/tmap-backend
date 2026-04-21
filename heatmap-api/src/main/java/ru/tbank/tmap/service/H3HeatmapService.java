package ru.tbank.tmap.service;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.openapitools.model.ClusterDTO;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.domain.geo.H3Resolution;
import ru.tbank.tmap.repository.HeatmapQueryRepository;

@Service
@Transactional(readOnly = true)
public class H3HeatmapService implements HeatmapService {

    private static final int DEFAULT_WINDOW_MINUTES = 60;
    private static final int REFRESH_INTERVAL_MINUTES = 5;

    private final HeatmapQueryRepository heatmapQueryRepository;
    private final Clock clock;

    public H3HeatmapService(final HeatmapQueryRepository heatmapQueryRepository, final Clock clock) {
        this.heatmapQueryRepository = heatmapQueryRepository;
        this.clock = clock;
    }

    @Override
    public HeatmapResponse getHeatmapClusters(
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng,
            final H3Resolution resolution,
            final List<String> category,
            final int window
    ) {
        validateBounds(swLat, neLat);

        final Instant from = Instant.now(clock).minus(window, ChronoUnit.MINUTES);
        return new HeatmapResponse()
                .generatedAt(OffsetDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC))
                .refreshIntervalMinutes(REFRESH_INTERVAL_MINUTES)
                .aggregationWindowMinutes(window)
                .clusters(heatmapQueryRepository.findClusters(
                                swLat,
                                swLng,
                                neLat,
                                neLng,
                                resolution,
                                category,
                                from
                        ).stream()
                        .map(cluster -> new ClusterDTO(
                                Long.toHexString(cluster.h3Index()),
                                cluster.txCount(),
                                cluster.avgCheck().doubleValue()
                        ))
                        .toList());
    }

    @Override
    public Optional<ClusterDetailsResponse> getClusterDetails(final String h3Index, final H3Resolution resolution) {
        final Instant from = Instant.now(clock).minus(DEFAULT_WINDOW_MINUTES, ChronoUnit.MINUTES);
        final long dbH3Index = parseH3Index(h3Index);
        return heatmapQueryRepository.findClusterDetails(dbH3Index, resolution, from)
                .map(cluster -> new ClusterDetailsResponse()
                        .h3Index(Long.toHexString(cluster.h3Index()))
                        .resolution(cluster.resolution().getValue())
                        .txCount(cluster.txCount())
                        .avgCheck(cluster.avgCheck().doubleValue())
                        .sumAmount(cluster.sumAmount().doubleValue())
                        .createdAt(OffsetDateTime.ofInstant(cluster.updatedAt(), ZoneOffset.UTC)));
    }

    private void validateBounds(
            final double swLat,
            final double neLat
    ) {
        if (swLat >= neLat) {
            throw new IllegalArgumentException("Invalid map bounds");
        }
    }

    private long parseH3Index(final String h3Index) {
        try {
            return Long.parseUnsignedLong(h3Index, 16);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid h3Index", ex);
        }
    }
}
