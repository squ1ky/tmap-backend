package ru.tbank.tmap.shared.geo;

import lombok.Getter;

import java.util.Set;

@Getter
public enum H3Resolution {
    RES_6(6),
    RES_7(7),
    RES_8(8),
    RES_9(9);

    private final int value;

    H3Resolution(int value) {
        this.value = value;
    }

    public static final Set<H3Resolution> AGGREGATED = Set.of(RES_7, RES_8, RES_9);

    @SuppressWarnings("PMD.ShortMethodName")
    public static H3Resolution of(int value) {
        for (H3Resolution resolution : values()) {
            if (resolution.value == value) {
                return resolution;
            }
        }
        throw new IllegalArgumentException("Unsupported H3 resolution: " + value);
    }
}
