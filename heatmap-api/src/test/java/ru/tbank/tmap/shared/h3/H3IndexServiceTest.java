package ru.tbank.tmap.shared.h3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.uber.h3core.H3Core;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;

class H3IndexServiceTest {

    private static final long PARENT_RES_6 = Long.parseUnsignedLong("86115b22fffffff", 16);

    private static final BoundingBox KAZAN_BOUNDING_BOX =
            new BoundingBox(55.7481, 49.0664, 55.8402, 49.1912);

    private H3Core h3Core;
    private H3IndexService h3IndexService;

    @BeforeEach
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    void setUp() throws Exception {
        h3Core = H3Core.newInstance();
        h3IndexService = new H3IndexService(h3Core);
    }

    @Test
    void toH3_whenValidCoordinates_thenReturnsCellMatchingH3Core() {
        final double lat = 55.796127;
        final double lng = 49.106414;

        final long cell = h3IndexService.toH3(lat, lng, H3Resolution.RES_9);

        assertThat(cell).isEqualTo(h3Core.latLngToCell(lat, lng, H3Resolution.RES_9.getValue()));
    }

    @Test
    void bboxToParents_whenSmallViewportInsideSingleParentTile_thenReturnsThatParent() {
        final long childRes9 = h3Core.cellToChildren(PARENT_RES_6, H3Resolution.RES_9.getValue()).get(10);
        final var center = h3Core.cellToLatLng(childRes9);
        final double delta = 0.0005;
        final BoundingBox boundingBox = new BoundingBox(
                center.lat - delta,
                center.lng - delta,
                center.lat + delta,
                center.lng + delta
        );

        final List<Long> parents = h3IndexService.bboxToParents(
                boundingBox, H3Resolution.RES_9, H3Resolution.RES_6
        );

        assertThat(parents).isNotEmpty();
        assertThat(parents).contains(PARENT_RES_6);
    }

    @Test
    void bboxToParents_whenLargeViewport_thenReturnsMultipleDistinctParents() {
        final List<Long> parents = h3IndexService.bboxToParents(
                KAZAN_BOUNDING_BOX, H3Resolution.RES_9, H3Resolution.RES_6
        );

        assertThat(parents).hasSizeGreaterThan(1);
        assertThat(parents).doesNotHaveDuplicates();
        for (Long parent : parents) {
            assertThat(h3Core.getResolution(parent)).isEqualTo(H3Resolution.RES_6.getValue());
        }
    }

    @Test
    void bboxToParents_whenViewportStraddlesTwoParentTiles_thenReturnsBothParents() {
        final var centerA = h3Core.cellToLatLng(PARENT_RES_6);
        final long neighborRes6 = h3Core.gridDisk(PARENT_RES_6, 1).stream()
                .filter(cell -> cell != PARENT_RES_6)
                .findFirst()
                .orElseThrow();
        final var centerB = h3Core.cellToLatLng(neighborRes6);

        final double minLat = Math.min(centerA.lat, centerB.lat) - 0.001;
        final double maxLat = Math.max(centerA.lat, centerB.lat) + 0.001;
        final double minLng = Math.min(centerA.lng, centerB.lng) - 0.001;
        final double maxLng = Math.max(centerA.lng, centerB.lng) + 0.001;
        final BoundingBox boundingBox = new BoundingBox(minLat, minLng, maxLat, maxLng);

        final List<Long> parents = h3IndexService.bboxToParents(
                boundingBox, H3Resolution.RES_9, H3Resolution.RES_6
        );

        assertThat(parents).contains(PARENT_RES_6, neighborRes6);
    }

    @Test
    void bboxToParents_whenProbeResolutionNotFinerThanParent_thenThrowsIllegalArgument() {
        assertThatThrownBy(() -> h3IndexService.bboxToParents(
                KAZAN_BOUNDING_BOX, H3Resolution.RES_6, H3Resolution.RES_6
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cellToParent_whenChildCell_thenReturnsExpectedParent() {
        final long childRes9 = h3Core.cellToChildren(PARENT_RES_6, H3Resolution.RES_9.getValue()).getFirst();

        final long parent = h3IndexService.cellToParent(childRes9, H3Resolution.RES_6);

        assertThat(parent).isEqualTo(PARENT_RES_6);
    }
}