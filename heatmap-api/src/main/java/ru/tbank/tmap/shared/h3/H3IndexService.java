package ru.tbank.tmap.shared.h3;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.shared.geo.H3Resolution;

import java.util.List;

@Service
@RequiredArgsConstructor
public class H3IndexService {

    private final H3Core h3;

    public long toH3(double lat, double lng, H3Resolution resolution) {
        return h3.latLngToCell(lat, lng, resolution.getValue());
    }

    public List<Long> bboxToCells(BoundingBox bbox, H3Resolution resolution) {
        List<LatLng> outline = List.of(
                new LatLng(bbox.swLat(), bbox.swLng()),
                new LatLng(bbox.swLat(), bbox.neLng()),
                new LatLng(bbox.neLat(), bbox.neLng()),
                new LatLng(bbox.neLat(), bbox.swLng())
        );
        return h3.polygonToCells(outline, List.of(), resolution.getValue());
    }

    public long cellToParent(long cell, H3Resolution resolution) {
        return h3.cellToParent(cell, resolution.getValue());
    }
}
