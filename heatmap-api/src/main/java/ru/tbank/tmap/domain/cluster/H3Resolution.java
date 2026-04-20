package ru.tbank.tmap.domain.cluster;

import lombok.Getter;

@Getter
public enum H3Resolution {
    RES_7(7),
    RES_8(8),
    RES_9(9);

    private final int value;

    H3Resolution(int value) {
        this.value = value;
    }

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
