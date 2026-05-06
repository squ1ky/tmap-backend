package ru.tbank.tmap.venue.business.photo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenuePhotoUpdater {

    private final VenueRepository venueRepository;

    /**
     * Sets a new photo object key for the venue and triggers re-moderation if it was ACTIVE.
     *
     * @return the previous object key (null if the venue had no photo before)
     */
    @Transactional
    public String swapPhotoKey(final UUID venueId, final UUID ownerId, final String newObjectKey) {
        final Venue venue = venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
        final String oldObjectKey = venue.getPhotoObjectKey();
        venue.setPhotoObjectKey(newObjectKey);
        venue.markEditedForReview();
        return oldObjectKey;
    }

    /**
     * Clears the photo object key for the venue.
     *
     * @return the previous object key (null if the venue had no photo)
     */
    @Transactional
    public String clearPhotoKey(final UUID venueId, final UUID ownerId) {
        final Venue venue = venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
        final String oldObjectKey = venue.getPhotoObjectKey();
        venue.setPhotoObjectKey(null);
        return oldObjectKey;
    }
}
