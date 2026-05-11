package ru.tbank.tmap.heatmap.application.service;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.heatmap.presentation.dto.HeatmapClusters;
import ru.tbank.tmap.heatmap.application.query.ClusterDetailsAggregate;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryQueryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class H3HeatmapService {

    private static final int DEFAULT_WINDOW_MINUTES = 60;
    private static final int REFRESH_INTERVAL_MINUTES = 5;

    private final ClusterHistoryQueryRepository heatmapQueryRepository;
    private final MinioUrlBuilder minioUrlBuilder;
    private final Clock clock;

    public HeatmapClusters getHeatmapClusters(
            final BoundingBox boundingBox,
            final H3Resolution resolution,
            final int window
    ) {
        final Instant now = Instant.now(clock);
        final Instant from = now.minus(window, ChronoUnit.MINUTES);
        final Instant currentHour = now.truncatedTo(ChronoUnit.HOURS);

        return new HeatmapClusters(
                OffsetDateTime.ofInstant(now, ZoneOffset.UTC),
                REFRESH_INTERVAL_MINUTES,
                window,
                heatmapQueryRepository.findClusters(
                        boundingBox,
                        resolution,
                        from,
                        currentHour
                )
        );
    }

    public Optional<ClusterDetailsAggregate> getClusterDetails(final String h3Index, final H3Resolution resolution) {
        final Instant now = Instant.now(clock);
        final Instant from = now.minus(DEFAULT_WINDOW_MINUTES, ChronoUnit.MINUTES);
        final Instant currentHour = now.truncatedTo(ChronoUnit.HOURS);

        final long dbH3Index = parseH3Index(h3Index);

        return heatmapQueryRepository.findClusterDetails(dbH3Index, resolution, from, currentHour)
                .map(this::resolveDistrictImageUrl);
    }

    private ClusterDetailsAggregate resolveDistrictImageUrl(final ClusterDetailsAggregate aggregate) {
        String districtImageUrl = minioUrlBuilder.buildPublicUrl(aggregate.districtImageUrl());
        return aggregate.withDistrictImageUrl(districtImageUrl);
    }

    private long parseH3Index(final String h3Index) {
        try {
            return Long.parseUnsignedLong(h3Index, 16);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid h3Index", ex);
        }
    }
}
