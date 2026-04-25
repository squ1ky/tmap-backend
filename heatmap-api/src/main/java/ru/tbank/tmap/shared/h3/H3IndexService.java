package ru.tbank.tmap.shared.h3;

import com.uber.h3core.H3Core;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.shared.geo.H3Resolution;

@Service
@RequiredArgsConstructor
public class H3IndexService {

    private final H3Core h3;

    public long toH3(double lat, double lng, H3Resolution resolution) {
        return h3.latLngToCell(lat, lng, resolution.getValue());
    }
}
