package ru.tbank.tmap.venue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.AbstractAggregateRoot;
import ru.tbank.tmap.venue.domain.event.VenuePhotoObsoleted;
import ru.tbank.tmap.venue.domain.exception.VenueModerationStateException;

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
public class Venue extends AbstractAggregateRoot<Venue> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @NotNull
    @Valid
    @Embedded
    @ToString.Include
    private VenueContent content;

    @Size(max = 255)
    @Column(name = "photo_object_key", length = 255)
    private String photoObjectKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @ToString.Include
    private VenueStatus status;

    @Column(name = "reject_reason", columnDefinition = "text")
    private String rejectReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public Venue(UUID id,
                 UUID ownerId,
                 VenueContent content,
                 VenueStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.content = Objects.requireNonNull(content, "content");
        this.status = status != null ? status : VenueStatus.PENDING;
        if (this.status == VenueStatus.PENDING_UPDATE) {
            throw new IllegalArgumentException("Venue cannot be created in PENDING_UPDATE status");
        }
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

    public void updatePhoto(String newObjectKey) {
        Objects.requireNonNull(newObjectKey, "newObjectKey");
        String oldObjectKey = this.photoObjectKey;
        this.photoObjectKey = newObjectKey;
        registerObsoletedPhoto(oldObjectKey, newObjectKey);
    }

    public void removePhoto() {
        String oldObjectKey = this.photoObjectKey;
        this.photoObjectKey = null;
        registerObsoletedPhoto(oldObjectKey, null);
    }

    public void approve() {
        if (status != VenueStatus.PENDING) {
            throw new VenueModerationStateException(id, status);
        }
        this.status = VenueStatus.ACTIVE;
        this.rejectReason = null;
    }

    public void approveWithUpdate(VenuePendingUpdate pendingUpdate) {
        Objects.requireNonNull(pendingUpdate, "pendingUpdate");
        if (status != VenueStatus.ACTIVE) {
            throw new VenueModerationStateException(id, status);
        }
        if (pendingUpdate.getStatus() != VenueStatus.PENDING_UPDATE) {
            throw new VenueModerationStateException(id, pendingUpdate.getStatus());
        }
        this.content = Objects.requireNonNull(pendingUpdate.getContent(), "pendingUpdate.content");
        this.rejectReason = null;

        final String newPhoto = pendingUpdate.getPendingPhotoObjectKey();
        if (newPhoto != null) {
            final String oldPhoto = this.photoObjectKey;
            this.photoObjectKey = newPhoto;
            registerObsoletedPhoto(oldPhoto, newPhoto);
        }
    }

    public void reject(String reason) {
        if (status != VenueStatus.PENDING) {
            throw new VenueModerationStateException(id, status);
        }
        this.status = VenueStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void applyContent(final VenueContent content) {
        this.content = Objects.requireNonNull(content, "content");
    }

    private void registerObsoletedPhoto(String oldKey, String newKey) {
        if (oldKey != null && !oldKey.equals(newKey)) {
            registerEvent(new VenuePhotoObsoleted(oldKey));
        }
    }
}
