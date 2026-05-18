package ru.tbank.tmap.venue.application.service.business.photo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BusinessVenuePhotoUpdater {

    private final VenueRepository venueRepository;
    private final VenuePendingUpdateRepository venuePendingUpdateRepository;

    /**
     * Sets a new photo for the venue.
     * <ul>
     *   <li>If the venue is ACTIVE, the new key is staged in a {@link VenuePendingUpdate}
     *       (re-moderation flow); the venue's current photo remains visible to the public
     *       until the moderator approves.</li>
     *   <li>Otherwise (PENDING / REJECTED), the photo is replaced directly on the venue,
     *       and REJECTED is moved back to PENDING.</li>
     * </ul>
     * <p>
     * Cleanup of orphaned S3 objects is handled via {@code VenuePhotoObsoleted} domain events
     * (published after commit).
     */
    @Transactional
    public void swapPhotoKey(final UUID venueId, final UUID ownerId, final String newObjectKey) {
        final Venue venue = venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));

        if (venue.getStatus() == VenueStatus.ACTIVE) {
            stagePhotoForModeration(venue, newObjectKey);
        } else {
            replacePhotoDirectly(venue, newObjectKey);
        }
    }

    @Transactional
    public void clearPhotoKey(final UUID venueId, final UUID ownerId) {
        final Venue venue = venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));

        venue.removePhoto();
        venueRepository.save(venue);

        venuePendingUpdateRepository.findByVenueId(venueId)
                .ifPresent(pending -> handlePendingOnPhotoClear(pending, venue));
    }

    private void stagePhotoForModeration(final Venue venue, final String newObjectKey) {
        final VenuePendingUpdate pending = venuePendingUpdateRepository.findByVenueId(venue.getId())
                .orElseGet(() -> VenuePendingUpdate.createForPhoto(venue, newObjectKey));
        pending.stagePhoto(newObjectKey);
        venuePendingUpdateRepository.save(pending);
    }

    private void handlePendingOnPhotoClear(final VenuePendingUpdate pending, final Venue venue) {
        if (pending.getPendingPhotoObjectKey() == null) {
            return;
        }

        if (pending.getContent().equals(venue.getContent())) {
            pending.discardStagedPhoto();
            venuePendingUpdateRepository.delete(pending);
            return;
        }

        pending.discardStagedPhoto();
        venuePendingUpdateRepository.save(pending);
    }

    private void replacePhotoDirectly(final Venue venue, final String newObjectKey) {
        venue.updatePhoto(newObjectKey);
        if (venue.getStatus() == VenueStatus.REJECTED) {
            venue.setStatus(VenueStatus.PENDING);
            venue.setRejectReason(null);
        }
        venueRepository.save(venue);
    }
}
