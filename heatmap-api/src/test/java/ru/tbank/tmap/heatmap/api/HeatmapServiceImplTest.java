package ru.tbank.tmap.heatmap.api;

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
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.heatmap.api.dto.ClusterDetailsResponse;
import ru.tbank.tmap.heatmap.api.dto.HeatmapClusterResponse;

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

        final List<HeatmapClusterResponse> response = heatmapService.getHeatmapClusters(
                55.7481,
                49.0664,
                55.8402,
                49.1912,
                8,
                60
        );

        assertThat(response).containsExactly(new HeatmapClusterResponse(
                "89115b22b0bffff",
                55.796127,
                49.106414,
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z")
        ));
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

        assertThat(response).contains(new ClusterDetailsResponse(
                "89115b22b0bffff",
                9,
                128,
                new BigDecimal("742.50"),
                new BigDecimal("95040.00"),
                Instant.parse("2026-04-17T10:15:00Z")
        ));
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
