package ru.tbank.tmap.shared.h3;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class H3IndexService {

    private final H3Core h3;

    public long toH3(double lat, double lng, H3Resolution resolution) {
        return h3.latLngToCell(lat, lng, resolution.getValue());
    }

    public List<Long> bboxToParents(BoundingBox bbox, H3Resolution fineResolution, H3Resolution parentResolution) {
        if (fineResolution.getValue() <= parentResolution.getValue()) {
            throw new IllegalArgumentException(
                    "fineResolution must be finer than parentResolution, got fine="
                            + fineResolution + ", parent=" + parentResolution);
        }

        List<LatLng> outline = buildOutline(bbox);

        List<Long> fineCells = h3.polygonToCells(outline, List.of(), fineResolution.getValue());

        Set<Long> parents = new HashSet<>();
        for (Long cell : fineCells) {
            parents.add(h3.cellToParent(cell, parentResolution.getValue()));
        }

        if (parents.isEmpty()) {
            parents.addAll(parentsFromBboxAnchors(bbox, parentResolution));
        }

        return new ArrayList<>(parents);
    }

    public long cellToParent(long cell, H3Resolution resolution) {
        return h3.cellToParent(cell, resolution.getValue());
    }

    private List<LatLng> buildOutline(BoundingBox bbox) {
        return List.of(
                new LatLng(bbox.swLat(), bbox.swLng()),
                new LatLng(bbox.swLat(), bbox.neLng()),
                new LatLng(bbox.neLat(), bbox.neLng()),
                new LatLng(bbox.neLat(), bbox.swLng())
        );
    }

    private List<Long> parentsFromBboxAnchors(BoundingBox bbox, H3Resolution parentResolution) {
        int parentRes = parentResolution.getValue();
        double centerLat = (bbox.swLat() + bbox.neLat()) / 2.0;
        double centerLng = (bbox.swLng() + bbox.neLng()) / 2.0;

        return List.of(
                h3.latLngToCell(bbox.swLat(), bbox.swLng(), parentRes),
                h3.latLngToCell(bbox.swLat(), bbox.neLng(), parentRes),
                h3.latLngToCell(bbox.neLat(), bbox.neLng(), parentRes),
                h3.latLngToCell(bbox.neLat(), bbox.swLng(), parentRes),
                h3.latLngToCell(centerLat, centerLng, parentRes)
        );
    }
}
