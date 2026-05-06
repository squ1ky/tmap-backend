package ru.tbank.tmap.venue.application.service.business.photo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.tbank.tmap.infrastructure.minio.ObjectStorageException;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.application.port.VenuePhotoStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessVenuePhotoService {

    private final VenueRepository venueRepository;
    private final BusinessVenuePhotoValidator venuePhotoValidator;
    private final VenuePhotoStorage venuePhotoStorage;
    private final BusinessVenuePhotoUpdater venuePhotoUpdater;

    public Venue uploadVenuePhoto(
            final UUID ownerId,
            final UUID venueId,
            final MultipartFile file
    ) {
        final String extension = venuePhotoValidator.validateAndGetExtension(file);

        venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));

        final String newObjectKey;
        try (InputStream stream = file.getInputStream()) {
            newObjectKey = venuePhotoStorage.upload(
                    venueId,
                    stream,
                    file.getSize(),
                    file.getContentType(),
                    extension
            );
        } catch (IOException e) {
            throw new ObjectStorageException("Failed to read uploaded file", e);
        }

        final String oldObjectKey;
        try {
            oldObjectKey = venuePhotoUpdater.swapPhotoKey(venueId, ownerId, newObjectKey);
        } catch (RuntimeException e) {
            safePhotoDelete(newObjectKey);
            throw e;
        }

        if (oldObjectKey != null && !oldObjectKey.equals(newObjectKey)) {
            safePhotoDelete(oldObjectKey);
        }

        return venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    public Venue deleteVenuePhoto(final UUID ownerId, final UUID venueId) {
        final String oldObjectKey = venuePhotoUpdater.clearPhotoKey(venueId, ownerId);
        if (oldObjectKey != null) {
            safePhotoDelete(oldObjectKey);
        }

        return venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private void safePhotoDelete(final String objectKey) {
        try {
            venuePhotoStorage.delete(objectKey);
        } catch (RuntimeException e) {
            log.warn("Failed to delete photo object: {}", objectKey, e);
        }
    }
}
