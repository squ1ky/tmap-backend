package ru.tbank.tmap.service;

import com.uber.h3core.H3Core;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class H3IndexService {

    public static final int RES_7 = 7;
    public static final int RES_8 = 8;
    public static final int RES_9 = 9;

    private final H3Core h3;

    public long toH3(double lat, double lng, int resolution) {
        return h3.latLngToCell(lat, lng, resolution);
    }
}
