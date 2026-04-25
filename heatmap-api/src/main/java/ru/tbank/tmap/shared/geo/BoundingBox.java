package ru.tbank.tmap.shared.geo;

public record BoundingBox(double swLat, double swLng, double neLat, double neLng) {
    public BoundingBox {
        if (swLat >= neLat) {
            throw new IllegalArgumentException("Invalid map bounds");
        }
    }

    public boolean contains(double lat, double lng) {
        final boolean withinLatitude = lat >= swLat && lat <= neLat;
        final boolean withinLongitude;
        if (swLng <= neLng) {
            withinLongitude = lng >= swLng && lng <= neLng;
        } else {
            withinLongitude = lng >= swLng || lng <= neLng;
        }
        return withinLatitude && withinLongitude;
    }
}
