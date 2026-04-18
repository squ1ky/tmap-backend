package ru.tbank.tmap.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.openapitools.model.ClusterDetailsResponse;
import org.openapitools.model.HeatmapResponse;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.repository.model.ClusterDetailsAggregate;
import ru.tbank.tmap.repository.model.HeatmapClusterAggregate;

class HeatmapServiceImplTest {

    @Mock
    private HeatmapQueryRepository heatmapQueryRepository;

    private HeatmapServiceImpl heatmapService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        heatmapService = new HeatmapServiceImpl(
                heatmapQueryRepository,
                Clock.fixed(Instant.parse("2026-04-17T10:20:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldMapAggregatedClustersToResponse() {
        given(heatmapQueryRepository.findClusters(
                55.7481,
                49.0664,
                55.8402,
                49.1912,
                8,
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

        final HeatmapResponse response = heatmapService.getHeatmapClusters(
                55.7481,
                49.0664,
                55.8402,
                49.1912,
                8,
                60
        );

        assertThat(response.getAggregationWindowMinutes()).isEqualTo(60);
        assertThat(response.getRefreshIntervalMinutes()).isEqualTo(5);
        assertThat(response.getClusters()).hasSize(1);
        assertThat(response.getClusters().getFirst().getH3Index()).isEqualTo("89115b22b0bffff");
        assertThat(response.getClusters().getFirst().getTxCount()).isEqualTo(128);
        assertThat(response.getClusters().getFirst().getAvgCheck()).isEqualTo(742.50);
    }

    @Test
    void shouldMapClusterDetailsToResponse() {
        given(heatmapQueryRepository.findClusterDetails(
                Long.parseUnsignedLong("89115b22b0bffff", 16),
                9,
                Instant.parse("2026-04-17T09:20:00Z")
        )).willReturn(Optional.of(new ClusterDetailsAggregate(
                Long.parseUnsignedLong("89115b22b0bffff", 16),
                9,
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z")
        )));

        final Optional<ClusterDetailsResponse> response =
                heatmapService.getClusterDetails("89115b22b0bffff", 9);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getH3Index()).isEqualTo("89115b22b0bffff");
        assertThat(response.orElseThrow().getResolution()).isEqualTo(9);
        assertThat(response.orElseThrow().getTxCount()).isEqualTo(128);
        assertThat(response.orElseThrow().getAvgCheck()).isEqualTo(742.50);
        assertThat(response.orElseThrow().getSumAmount()).isEqualTo(95040.00);
    }

    @Test
    void shouldRejectInvalidBounds() {
        assertThatThrownBy(() -> heatmapService.getHeatmapClusters(
                55.9,
                49.2,
                55.8,
                49.1,
                8,
                60
        )).isInstanceOf(ResponseStatusException.class);
    }
}
