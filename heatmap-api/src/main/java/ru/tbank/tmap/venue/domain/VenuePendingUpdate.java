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
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Persistable;
import jakarta.persistence.Transient;
import ru.tbank.tmap.venue.domain.event.VenuePhotoObsoleted;
import ru.tbank.tmap.venue.domain.exception.VenueModerationStateException;

@Entity
@Table(name = "venue_pending_updates")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class VenuePendingUpdate extends AbstractAggregateRoot<VenuePendingUpdate> implements Persistable<UUID> {

    @Id
    @Column(name = "venue_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID venueId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @NotNull
    @Valid
    @Embedded
    private VenueContent content;

    @Size(max = 255)
    @Column(name = "pending_photo_object_key", length = 255)
    private String pendingPhotoObjectKey;

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

    @Transient
    private boolean newEntity = true;

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

    @PostPersist
    @PostLoad
        /* default */ void markNotNew() {
        newEntity = false;
    }

    private VenuePendingUpdate(final Venue venue, final VenueContent content) {
        this.venue = venue;
        this.venueId = venue.getId();
        this.content = content;
    }

    public static VenuePendingUpdate create(final Venue venue, final VenueContent content) {
        Objects.requireNonNull(venue, "venue");
        Objects.requireNonNull(content, "content");
        return new VenuePendingUpdate(venue, content);
    }

    public static VenuePendingUpdate createForPhoto(final Venue venue, final String newPhotoObjectKey) {
        Objects.requireNonNull(venue, "venue");
        Objects.requireNonNull(newPhotoObjectKey, "newPhotoObjectKey");
        VenuePendingUpdate pendingUpdate = new VenuePendingUpdate(venue, venue.getContent());
        pendingUpdate.pendingPhotoObjectKey = newPhotoObjectKey;
        return pendingUpdate;
    }

    @Override
    public UUID getId() {
        return venueId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void reject(String reason) {
        if (status != VenueStatus.PENDING_UPDATE) {
            throw new VenueModerationStateException(venueId, status);
        }
        this.status = VenueStatus.REJECTED;
        this.rejectReason = reason;
        if (this.pendingPhotoObjectKey != null) {
            registerEvent(new VenuePhotoObsoleted(this.pendingPhotoObjectKey));
            this.pendingPhotoObjectKey = null;
        }
    }

    public void stagePhoto(final String newObjectKey) {
        Objects.requireNonNull(newObjectKey, "newObjectKey");
        String previous = this.pendingPhotoObjectKey;
        this.pendingPhotoObjectKey = newObjectKey;
        this.status = VenueStatus.PENDING_UPDATE;
        this.rejectReason = null;
        if (previous != null && !previous.equals(newObjectKey)) {
            registerEvent(new VenuePhotoObsoleted(previous));
        }
    }

    public void applyContent(final VenueContent content) {
            this.content = Objects.requireNonNull(content, "content");
            this.status = VenueStatus.PENDING_UPDATE;
            this.rejectReason = null;
    }
}
