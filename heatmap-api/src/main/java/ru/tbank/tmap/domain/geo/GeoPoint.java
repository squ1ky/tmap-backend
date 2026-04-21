package ru.tbank.tmap.domain.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class GeoPoint {

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lng", nullable = false)
    private double lng;

    @SuppressWarnings("PMD.ShortMethodName")
    public static GeoPoint of(double lat, double lng) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("lat must be in [-90, 90], got " + lat);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("lng must be in [-180, 180], got " + lng);
        }
        return new GeoPoint(lat, lng);
    }
}
