package ru.tbank.tmap.venue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.tbank.tmap.shared.geo.GeoPoint;

@Embeddable
public record VenueContent(

        @NotBlank
        @Size(max = 255)
        @Column(name = "name", nullable = false, length = 255)
        String name,

        @NotBlank
        @Size(max = 255)
        @Column(name = "address", nullable = false, length = 255)
        String address,

        @NotNull
        @Embedded
        GeoPoint location,

        @Column(name = "h3_res9", nullable = false)
        long h3Res9,

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(name = "category", nullable = false, length = 64)
        VenueCategory category,

        @Column(name = "description", columnDefinition = "text")
        String description,

        @Size(max = 255)
        @Column(name = "dish_of_day", length = 255)
        String dishOfDay,

        @Size(max = 255)
        @Column(name = "music", length = 255)
        String music
) {
}
