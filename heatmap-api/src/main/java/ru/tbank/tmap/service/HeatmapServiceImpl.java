package ru.tbank.tmap.service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.openapitools.model.ClusterDTO;
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.repository.HeatmapQueryRepository;

@Service
public class HeatmapServiceImpl implements HeatmapService {

    private static final int DEFAULT_WINDOW_MINUTES = 60;
    private static final int REFRESH_INTERVAL_MINUTES = 5;
    private static final int MIN_SUPPORTED_RESOLUTION = 7;
    private static final int MAX_SUPPORTED_RESOLUTION = 9;

    private final HeatmapQueryRepository heatmapQueryRepository;
    private final Clock clock;

    public HeatmapServiceImpl(final HeatmapQueryRepository heatmapQueryRepository, final Clock clock) {
        this.heatmapQueryRepository = heatmapQueryRepository;
        this.clock = clock;
    }

    @Override
    public HeatmapResponse getHeatmapClusters(
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng,
            final int resolution,
            final int window
    ) {
        validateBounds(swLat, swLng, neLat, neLng);
        validateResolution(resolution);
        validateWindow(window);

        final Instant from = Instant.now(clock).minusSeconds(window * 60L);
        return new HeatmapResponse()
                .generatedAt(OffsetDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC))
                .refreshIntervalMinutes(REFRESH_INTERVAL_MINUTES)
                .aggregationWindowMinutes(window)
                .clusters(heatmapQueryRepository.findClusters(swLat, swLng, neLat, neLng, resolution, from).stream()
                        .map(cluster -> new ClusterDTO(
                                Long.toHexString(cluster.h3Index()),
                                cluster.txCount(),
                                cluster.avgCheck().doubleValue()
                        ))
                        .toList());
    }

    @Override
    public Optional<ClusterDetailsResponse> getClusterDetails(final String h3Index, final int resolution) {
        validateResolution(resolution);

        final Instant from = Instant.now(clock).minusSeconds(DEFAULT_WINDOW_MINUTES * 60L);
        final long dbH3Index = parseH3Index(h3Index);
        return heatmapQueryRepository.findClusterDetails(dbH3Index, resolution, from)
                .map(cluster -> new ClusterDetailsResponse()
                        .h3Index(Long.toHexString(cluster.h3Index()))
                        .resolution(cluster.resolution())
                        .txCount(cluster.txCount())
                        .avgCheck(cluster.avgCheck().doubleValue())
                        .sumAmount(cluster.sumAmount().doubleValue())
                        .createdAt(OffsetDateTime.ofInstant(cluster.updatedAt(), ZoneOffset.UTC)));
    }

    private void validateBounds(
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng
    ) {
        if (swLat >= neLat) {
            throw new IllegalArgumentException("Invalid map bounds");
        }
    }

    private void validateResolution(final int resolution) {
        if (resolution < MIN_SUPPORTED_RESOLUTION || resolution > MAX_SUPPORTED_RESOLUTION) {
            throw new IllegalArgumentException("Supported resolution range is 7..9");
        }
    }

    private void validateWindow(final int window) {
        if (window <= 0) {
            throw new IllegalArgumentException("Window must be positive");
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
