package ru.tbank.tmap.heatmap.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.tbank.tmap.heatmap.application.port.cache.HeatmapClusterReader;
import ru.tbank.tmap.heatmap.application.query.ClusterDetailsAggregate;
import ru.tbank.tmap.heatmap.application.query.HeatmapClusterAggregate;
import ru.tbank.tmap.heatmap.domain.ClusterHistoryQueryRepository;
import ru.tbank.tmap.heatmap.presentation.dto.HeatmapClusters;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;

class H3HeatmapServiceTest {

    private static final BoundingBox KAZAN_BOUNDING_BOX =
            new BoundingBox(55.7481, 49.0664, 55.8402, 49.1912);

    private static final Instant FIXED_NOW = Instant.parse("2026-04-17T10:20:00Z");
    private static final Instant WINDOW_FROM = Instant.parse("2026-04-17T09:20:00Z");
    private static final Instant CURRENT_HOUR = Instant.parse("2026-04-17T10:00:00Z");
    private static final int WINDOW_MINUTES = 60;

    private static final String H3_INDEX_HEX = "89115b22b0bffff";
    private static final long H3_INDEX = Long.parseUnsignedLong(H3_INDEX_HEX, 16);

    private static final List<Long> PARENTS = List.of(
            Long.parseUnsignedLong("86115b227ffffff", 16),
            Long.parseUnsignedLong("86115b22fffffff", 16)
    );

    @Mock
    private ClusterHistoryQueryRepository heatmapQueryRepository;

    @Mock
    private HeatmapClusterReader clusterReader;

    @Mock
    private H3IndexService h3IndexService;

    @Mock
    private MinioUrlBuilder minioUrlBuilder;

    private H3HeatmapService heatmapService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        heatmapService = new H3HeatmapService(
                heatmapQueryRepository,
                clusterReader,
                h3IndexService,
                minioUrlBuilder,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void getHeatmapClusters_whenReaderReturnsClusters_thenReturnHeatmapData() {
        given(h3IndexService.bboxToCells(KAZAN_BOUNDING_BOX, H3Resolution.RES_6))
                .willReturn(PARENTS);
        given(clusterReader.read(
                PARENTS,
                H3Resolution.RES_8,
                WINDOW_MINUTES,
                WINDOW_FROM,
                CURRENT_HOUR
        )).willReturn(List.of(new HeatmapClusterAggregate(
                H3_INDEX,
                55.796127,
                49.106414,
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z"),
                false,
                null
        )));

        final HeatmapClusters response = heatmapService.getHeatmapClusters(
                KAZAN_BOUNDING_BOX, H3Resolution.RES_8, WINDOW_MINUTES
        );

        assertThat(response.aggregationWindowMinutes()).isEqualTo(WINDOW_MINUTES);
        assertThat(response.refreshIntervalMinutes()).isEqualTo(5);
        assertThat(response.clusters()).hasSize(1);

        final HeatmapClusterAggregate cluster = response.clusters().getFirst();

        assertThat(Long.toHexString(cluster.h3Index())).isEqualTo(H3_INDEX_HEX);
        assertThat(cluster.txCount()).isEqualTo(128);
        assertThat(cluster.avgCheck()).isEqualByComparingTo("742.50");
        assertThat(cluster.isAnomaly()).isFalse();
        assertThat(cluster.anomalyRatio()).isNull();
    }

    @Test
    void getHeatmapClusters_whenClusterIsAnomalous_thenReturnAnomalyFlagAndRatio() {
        given(h3IndexService.bboxToCells(KAZAN_BOUNDING_BOX, H3Resolution.RES_6))
                .willReturn(PARENTS);
        given(clusterReader.read(
                PARENTS,
                H3Resolution.RES_8,
                WINDOW_MINUTES,
                WINDOW_FROM,
                CURRENT_HOUR
        )).willReturn(List.of(new HeatmapClusterAggregate(
                H3_INDEX,
                55.796127,
                49.106414,
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z"),
                true,
                new BigDecimal("3.40")
        )));

        final HeatmapClusters response = heatmapService.getHeatmapClusters(
                KAZAN_BOUNDING_BOX, H3Resolution.RES_8, WINDOW_MINUTES
        );

        assertThat(response.clusters()).hasSize(1);

        final HeatmapClusterAggregate cluster = response.clusters().getFirst();

        assertThat(cluster.isAnomaly()).isTrue();
        assertThat(cluster.anomalyRatio()).isEqualByComparingTo("3.40");
    }

    @Test
    void getHeatmapClusters_whenClusterCenterOutsideBoundingBox_thenFiltersItOut() {
        given(h3IndexService.bboxToCells(KAZAN_BOUNDING_BOX, H3Resolution.RES_6))
                .willReturn(PARENTS);
        given(clusterReader.read(
                PARENTS,
                H3Resolution.RES_8,
                WINDOW_MINUTES,
                WINDOW_FROM,
                CURRENT_HOUR
        )).willReturn(List.of(
                new HeatmapClusterAggregate(
                        H3_INDEX,
                        55.796127,
                        49.106414,
                        50,
                        new BigDecimal("100.00"),
                        new BigDecimal("5000.00"),
                        Instant.parse("2026-04-17T10:15:00Z"),
                        false,
                        null
                ),
                new HeatmapClusterAggregate(
                        0xDEADBEEFL,
                        56.0000,
                        50.0000,
                        50,
                        new BigDecimal("100.00"),
                        new BigDecimal("5000.00"),
                        Instant.parse("2026-04-17T10:15:00Z"),
                        false,
                        null
                )
        ));

        final HeatmapClusters response = heatmapService.getHeatmapClusters(
                KAZAN_BOUNDING_BOX, H3Resolution.RES_8, WINDOW_MINUTES
        );

        assertThat(response.clusters()).hasSize(1);
        assertThat(response.clusters().getFirst().h3Index()).isEqualTo(H3_INDEX);
    }

    @Test
    void getClusterDetails_whenRepositoryReturnsCluster_thenReturnClusterDetails() {
        given(heatmapQueryRepository.findClusterDetails(
                H3_INDEX,
                H3Resolution.RES_9,
                WINDOW_FROM,
                CURRENT_HOUR
        )).willReturn(Optional.of(new ClusterDetailsAggregate(
                H3_INDEX,
                H3Resolution.RES_9,
                "Вахитовский район",
                "districts/kazan/vahitovsky.jpg",
                Instant.parse("2026-04-17T10:00:00Z"),
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z"),
                false,
                null,
                null
        )));
        given(minioUrlBuilder.buildPublicUrl("districts/kazan/vahitovsky.jpg"))
                .willReturn("http://localhost:9000/tmap/districts/kazan/vahitovsky.jpg");

        final Optional<ClusterDetailsAggregate> response =
                heatmapService.getClusterDetails(H3_INDEX_HEX, H3Resolution.RES_9);

        assertThat(response).isPresent();

        final ClusterDetailsAggregate cluster = response.orElseThrow();

        assertThat(Long.toHexString(cluster.h3Index())).isEqualTo(H3_INDEX_HEX);
        assertThat(cluster.resolution()).isEqualTo(H3Resolution.RES_9);
        assertThat(cluster.districtName()).isEqualTo("Вахитовский район");
        assertThat(cluster.districtImageUrl())
                .isEqualTo("http://localhost:9000/tmap/districts/kazan/vahitovsky.jpg");
        assertThat(cluster.hourBucket()).isEqualTo(Instant.parse("2026-04-17T10:00:00Z"));
        assertThat(cluster.txCount()).isEqualTo(128);
        assertThat(cluster.avgCheck()).isEqualByComparingTo("742.50");
        assertThat(cluster.sumAmount()).isEqualByComparingTo("95040.00");
        assertThat(cluster.isAnomaly()).isFalse();
        assertThat(cluster.anomalyRatio()).isNull();
        assertThat(cluster.baselineAvg()).isNull();
    }

    @Test
    void getClusterDetails_whenDistrictHasNoPhoto_thenDistrictImageUrlIsNull() {
        given(heatmapQueryRepository.findClusterDetails(
                H3_INDEX,
                H3Resolution.RES_9,
                WINDOW_FROM,
                CURRENT_HOUR
        )).willReturn(Optional.of(new ClusterDetailsAggregate(
                H3_INDEX,
                H3Resolution.RES_9,
                "Вахитовский район",
                null,
                Instant.parse("2026-04-17T10:00:00Z"),
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z"),
                false,
                null,
                null
        )));
        given(minioUrlBuilder.buildPublicUrl(null)).willReturn(null);

        final Optional<ClusterDetailsAggregate> response =
                heatmapService.getClusterDetails(H3_INDEX_HEX, H3Resolution.RES_9);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().districtImageUrl()).isNull();
    }

    @Test
    void getClusterDetails_whenClusterIsAnomalous_thenReturnAnomalyFieldsForPopup() {
        given(heatmapQueryRepository.findClusterDetails(
                H3_INDEX,
                H3Resolution.RES_9,
                WINDOW_FROM,
                CURRENT_HOUR
        )).willReturn(Optional.of(new ClusterDetailsAggregate(
                H3_INDEX,
                H3Resolution.RES_9,
                "Вахитовский район",
                "districts/kazan/vahitovsky.jpg",
                Instant.parse("2026-04-17T10:00:00Z"),
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z"),
                true,
                new BigDecimal("3.40"),
                new BigDecimal("37.60")
        )));
        given(minioUrlBuilder.buildPublicUrl("districts/kazan/vahitovsky.jpg"))
                .willReturn("http://localhost:9000/tmap/districts/kazan/vahitovsky.jpg");

        final Optional<ClusterDetailsAggregate> response =
                heatmapService.getClusterDetails(H3_INDEX_HEX, H3Resolution.RES_9);

        assertThat(response).isPresent();

        final ClusterDetailsAggregate cluster = response.orElseThrow();

        assertThat(cluster.txCount()).isEqualTo(128);
        assertThat(cluster.isAnomaly()).isTrue();
        assertThat(cluster.anomalyRatio()).isEqualByComparingTo("3.40");
        assertThat(cluster.baselineAvg()).isEqualByComparingTo("37.60");
    }
}