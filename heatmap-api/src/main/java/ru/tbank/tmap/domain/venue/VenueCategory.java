package ru.tbank.tmap.domain.venue;

import lombok.Getter;

@Getter
public enum VenueCategory {
    FOOD("Еда"),
    ENTERTAINMENT("Развлечения"),
    SHOPPING("Шоппинг");

    private final String displayName;

    VenueCategory(String displayName) {
        this.displayName = displayName;
    }

    public static VenueCategory fromString(String value) {
        for (VenueCategory category : values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
