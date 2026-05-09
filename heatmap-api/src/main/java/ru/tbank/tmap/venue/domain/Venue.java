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
import ru.tbank.tmap.venue.domain.exception.VenueModerationStateException;

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

    @OneToMany(
            mappedBy = "venue",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("createdAt DESC, id DESC")
    private List<VenuePromo> promos = new ArrayList<>();

    @Builder
    public Venue(UUID id,
                 UUID ownerId,
                 VenueContent content,
                 VenueStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.content = Objects.requireNonNull(content, "content");
        this.status = status != null ? status : VenueStatus.PENDING;
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
}
