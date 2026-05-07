package ru.tbank.tmap.venue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.venue.business.VenueCreateCommand;
import ru.tbank.tmap.venue.business.VenueUpdateCommand;

import java.time.OffsetDateTime;
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

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

    public Venue(UUID id,
                 User owner,
                 String name,
                 String address,
                 GeoPoint location,
                 long h3Res9,
                 VenueCategory category) {
        this.id = Objects.requireNonNull(id, "id");
        this.owner = Objects.requireNonNull(owner, "owner");
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

    public void markEditedForReview() {
        if (status == VenueStatus.ACTIVE) {
            status = VenueStatus.PENDING_UPDATE;
        }
    }

    public void applyPendingUpdate(final VenuePendingUpdate pendingUpdate) {
        this.name = Objects.requireNonNull(pendingUpdate.getName(), "pendingUpdate.name");
        this.address = Objects.requireNonNull(pendingUpdate.getAddress(), "pendingUpdate.address");
        this.location = Objects.requireNonNull(pendingUpdate.getLocation(), "pendingUpdate.location");
        this.h3Res9 = pendingUpdate.getH3Res9();
        this.category = Objects.requireNonNull(pendingUpdate.getCategory(), "pendingUpdate.category");
        this.description = pendingUpdate.getDescription();
        this.dishOfDay = pendingUpdate.getDishOfDay();
        this.music = pendingUpdate.getMusic();
    }

    public static Venue create(
            final UUID id,
            final User owner,
            final VenueCreateCommand command,
            final long h3Res9
    ) {
        final Venue venue = new Venue(
                id,
                owner,
                command.name(),
                command.address(),
                command.location(),
                h3Res9,
                command.category()
        );
        venue.description = command.description();
        venue.dishOfDay = command.dishOfDay();
        venue.music = command.music();
        return venue;
    }

    public void applyUpdate(final VenueUpdateCommand command, final long h3Res9) {
        this.name = Objects.requireNonNull(command.name(), "command.name");
        this.address = Objects.requireNonNull(command.address(), "command.address");
        this.location = Objects.requireNonNull(command.location(), "command.location");
        this.h3Res9 = h3Res9;
        this.category = Objects.requireNonNull(command.category(), "command.category");
        this.description = command.description();
        this.dishOfDay = command.dishOfDay();
        this.music = command.music();
    }
}
