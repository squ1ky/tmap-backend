package ru.tbank.tmap.heatmap;

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
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.heatmap.repository.HeatmapQueryRepository;
import ru.tbank.tmap.heatmap.cluster.ClusterDetailsAggregate;

class H3HeatmapServiceTest {

    @Mock
    private HeatmapQueryRepository heatmapQueryRepository;

    private H3HeatmapService heatmapService;

    private static final BoundingBox KAZAN_BOUNDING_BOX =
            new BoundingBox(55.7481, 49.0664, 55.8402, 49.1912);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        heatmapService = new H3HeatmapService(
                heatmapQueryRepository,
                Clock.fixed(Instant.parse("2026-04-17T10:20:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void getHeatmapClusters_whenRepositoryReturnsClusters_thenReturnHeatmapData() {
        given(heatmapQueryRepository.findClusters(
                KAZAN_BOUNDING_BOX,
                H3Resolution.RES_8,
                null,
                Instant.parse("2026-04-17T09:20:00Z")
        )).willReturn(List.of(new HeatmapClusterAggregate(
                Long.parseUnsignedLong("89115b22b0bffff", 16),
                55.796127,
                49.106414,
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z")
        )));

        final HeatmapClusters response = heatmapService.getHeatmapClusters(
                KAZAN_BOUNDING_BOX,
                H3Resolution.RES_8,
                null,
                60
        );

        assertThat(response.aggregationWindowMinutes()).isEqualTo(60);
        assertThat(response.refreshIntervalMinutes()).isEqualTo(5);
        assertThat(response.clusters()).hasSize(1);
        assertThat(Long.toHexString(response.clusters().getFirst().h3Index())).isEqualTo("89115b22b0bffff");
        assertThat(response.clusters().getFirst().txCount()).isEqualTo(128);
        assertThat(response.clusters().getFirst().avgCheck()).isEqualByComparingTo("742.50");
    }

    @Test
    void getClusterDetails_whenRepositoryReturnsCluster_thenReturnClusterDetails() {
        given(heatmapQueryRepository.findClusterDetails(
                Long.parseUnsignedLong("89115b22b0bffff", 16),
                H3Resolution.RES_9,
                Instant.parse("2026-04-17T09:20:00Z")
        )).willReturn(Optional.of(new ClusterDetailsAggregate(
                Long.parseUnsignedLong("89115b22b0bffff", 16),
                H3Resolution.RES_9,
                "Вахитовский район",
                "",
                "FOOD",
                Instant.parse("2026-04-17T10:00:00Z"),
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z")
        )));

        final Optional<ClusterDetailsAggregate> response =
                heatmapService.getClusterDetails("89115b22b0bffff", H3Resolution.RES_9);

        assertThat(response).isPresent();
        assertThat(Long.toHexString(response.orElseThrow().h3Index())).isEqualTo("89115b22b0bffff");
        assertThat(response.orElseThrow().resolution()).isEqualTo(H3Resolution.RES_9);
        assertThat(response.orElseThrow().districtName()).isEqualTo("Вахитовский район");
        assertThat(response.orElseThrow().districtImageUrl()).isEmpty();
        assertThat(response.orElseThrow().category()).isEqualTo("FOOD");
        assertThat(response.orElseThrow().hourBucket())
                .isEqualTo(Instant.parse("2026-04-17T10:00:00Z"));
        assertThat(response.orElseThrow().txCount()).isEqualTo(128);
        assertThat(response.orElseThrow().avgCheck()).isEqualByComparingTo("742.50");
        assertThat(response.orElseThrow().sumAmount()).isEqualByComparingTo("95040.00");
    }
}
