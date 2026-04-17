package ru.tbank.tmap.heatmap.api;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.heatmap.api.dto.HeatmapResponse;

@Service
public class HeatmapServiceImpl implements HeatmapService {

    private static final int DEFAULT_WINDOW_MINUTES = 60;

    @Override
    public HeatmapResponse getHeatmap() {
        return new HeatmapResponse(DEFAULT_WINDOW_MINUTES, Instant.now(), List.of());
    }
}
