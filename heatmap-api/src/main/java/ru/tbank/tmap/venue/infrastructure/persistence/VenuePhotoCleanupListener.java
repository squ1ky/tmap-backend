package ru.tbank.tmap.venue.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.tbank.tmap.venue.application.port.VenuePhotoStorage;
import ru.tbank.tmap.venue.domain.event.VenuePhotoObsoleted;

@Slf4j
@Component
@RequiredArgsConstructor
public class VenuePhotoCleanupListener {

    private final VenuePhotoStorage venuePhotoStorage;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVenuePhotoObsoleted(VenuePhotoObsoleted event) {
        try {
            venuePhotoStorage.delete(event.objectKey());
        } catch (RuntimeException e) {
            log.warn("Failed to delete obsoleted photo object: {}", event.objectKey(), e);
        }
    }
}
