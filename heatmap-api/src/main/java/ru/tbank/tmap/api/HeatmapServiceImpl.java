package ru.tbank.tmap.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.api.dto.ClusterDetailsResponse;
import ru.tbank.tmap.api.dto.HeatmapClusterResponse;

@Service
public class HeatmapServiceImpl implements HeatmapService {

    private static final int DEFAULT_WINDOW_MINUTES = 60;
    private static final int MIN_SUPPORTED_RESOLUTION = 7;
    private static final int MAX_SUPPORTED_RESOLUTION = 9;

    private final HeatmapQueryRepository heatmapQueryRepository;
    private final Clock clock;

    public HeatmapServiceImpl(final HeatmapQueryRepository heatmapQueryRepository, final Clock clock) {
        this.heatmapQueryRepository = heatmapQueryRepository;
        this.clock = clock;
    }

    @Override
    public List<HeatmapClusterResponse> getHeatmapClusters(
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
        return heatmapQueryRepository.findClusters(swLat, swLng, neLat, neLng, resolution, from).stream()
                .map(cluster -> new HeatmapClusterResponse(
                        Long.toHexString(cluster.h3Index()),
                        cluster.centerLat(),
                        cluster.centerLng(),
                        cluster.txCount(),
                        cluster.avgCheck(),
                        cluster.sumAmount(),
                        cluster.updatedAt()
                ))
                .toList();
    }

    @Override
    public Optional<ClusterDetailsResponse> getClusterDetails(final String h3Index, final int resolution) {
        validateResolution(resolution);

        final Instant from = Instant.now(clock).minusSeconds(DEFAULT_WINDOW_MINUTES * 60L);
        final long dbH3Index = parseH3Index(h3Index);
        return heatmapQueryRepository.findClusterDetails(dbH3Index, resolution, from)
                .map(cluster -> new ClusterDetailsResponse(
                        Long.toHexString(cluster.h3Index()),
                        cluster.resolution(),
                        cluster.txCount(),
                        cluster.avgCheck(),
                        cluster.sumAmount(),
                        cluster.updatedAt()
                ));
    }

    private void validateBounds(
            final double swLat,
            final double swLng,
            final double neLat,
            final double neLng
    ) {
        if (swLat >= neLat || swLng >= neLng) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid map bounds");
        }
    }

    private void validateResolution(final int resolution) {
        if (resolution < MIN_SUPPORTED_RESOLUTION || resolution > MAX_SUPPORTED_RESOLUTION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Supported resolution range is 7..9"
            );
        }
    }

    private void validateWindow(final int window) {
        if (window <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Window must be positive");
        }
    }

    private long parseH3Index(final String h3Index) {
        try {
            return Long.parseUnsignedLong(h3Index, 16);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid h3Index", ex);
        }
    }
}
