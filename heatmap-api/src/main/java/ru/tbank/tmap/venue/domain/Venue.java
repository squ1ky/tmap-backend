package ru.tbank.tmap.venue.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.tbank.tmap.shared.geo.GeoPoint;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Venue {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false, length = 255)
    @ToString.Include
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

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    @ToString.Include
    private VenueCategory category;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Size(max = 255)
    @Column(name = "photo_object_key", length = 255)
    private String photoObjectKey;

    @Size(max = 255)
    @Column(name = "dish_of_day", length = 255)
    private String dishOfDay;

    @Size(max = 255)
    @Column(name = "music", length = 255)
    private String music;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @ToString.Include
    private VenueStatus status = VenueStatus.PENDING;

    @Column(name = "reject_reason", columnDefinition = "text")
    private String rejectReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "venue",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("createdAt DESC, id DESC")
    private List<VenuePromo> promos = new ArrayList<>();

    public Venue(UUID id,
                 UUID ownerId,
                 String name,
                 String address,
                 GeoPoint location,
                 long h3Res9,
                 VenueCategory category) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.name = Objects.requireNonNull(name, "name");
        this.address = Objects.requireNonNull(address, "address");
        this.location = Objects.requireNonNull(location, "location");
        this.h3Res9 = h3Res9;
        this.category = Objects.requireNonNull(category, "category");
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

    public String updatePhoto(String newObjectKey) {
        String oldObjectKey = this.photoObjectKey;
        this.photoObjectKey = newObjectKey;
        this.markEditedForReview();
        return oldObjectKey;
    }

    public String removePhoto() {
        String oldObjectKey = this.photoObjectKey;
        this.photoObjectKey = null;
        return oldObjectKey;
    }

    public void markEditedForReview() {
        if (status == VenueStatus.ACTIVE) {
            status = VenueStatus.PENDING_UPDATE;
        }
    }

    public void verify() {
        this.status = VenueStatus.ACTIVE;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reject reason must not be blank");
        }
        this.status = VenueStatus.REJECTED;
        this.rejectReason = reason.trim();
    }
}
