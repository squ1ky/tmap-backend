package ru.tbank.tmap.venue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.tbank.tmap.shared.geo.GeoPoint;

@Entity
@Table(name = "venue_pending_updates")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class VenuePendingUpdate {

    @Id
    @Column(name = "venue_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private java.util.UUID venueId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @NotNull
    @Embedded
    private GeoPoint location;

    @Column(name = "h3_res9", nullable = false)
    private long h3Res9;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    private VenueCategory category;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Size(max = 255)
    @Column(name = "dish_of_day", length = 255)
    private String dishOfDay;

    @Size(max = 255)
    @Column(name = "music", length = 255)
    private String music;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VenueStatus status = VenueStatus.PENDING_UPDATE;

    @Column(name = "reject_reason", columnDefinition = "text")
    private String rejectReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public VenuePendingUpdate(final Venue venue) {
        this.venue = venue;
        this.venueId = venue.getId();
    }

    @PrePersist
    /* default */ void onCreate() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    /* default */ void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
