package ru.tbank.tmap.domain.venue;

public enum VenueCategory {
    FOOD,
    ENTERTAINMENT,
    SHOPPING;

    public static VenueCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Venue category must not be null or blank");
        }

        String normalizedValue = value.trim();
        for (VenueCategory category : values()) {
            if (category.name().equalsIgnoreCase(normalizedValue)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unsupported venue category: " + value);
    }
}
