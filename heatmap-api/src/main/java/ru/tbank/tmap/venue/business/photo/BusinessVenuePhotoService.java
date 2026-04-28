package ru.tbank.tmap.venue.business.photo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.tbank.tmap.infrastructure.minio.ObjectStorageException;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserNotFoundException;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.repository.VenueRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessVenuePhotoService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final VenuePhotoValidator venuePhotoValidator;
    private final VenuePhotoStorage venuePhotoStorage;
    private final VenuePhotoUpdater venuePhotoUpdater;

    public Venue uploadVenuePhoto(
            final String ownerEmail,
            final UUID venueId,
            final MultipartFile file
    ) {
        final String extension = venuePhotoValidator.validateAndGetExtension(file);
        final User owner = findOwner(ownerEmail);

        venueRepository.findByIdAndOwnerId(venueId, owner.getId())
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
            oldObjectKey = venuePhotoUpdater.swapPhotoKey(venueId, owner.getId(), newObjectKey);
        } catch (RuntimeException e) {
            safePhotoDelete(newObjectKey);
            throw e;
        }

        if (oldObjectKey != null && !oldObjectKey.equals(newObjectKey)) {
            safePhotoDelete(oldObjectKey);
        }

        return venueRepository.findByIdAndOwnerId(venueId, owner.getId())
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    public Venue deleteVenuePhoto(final String ownerEmail, final UUID venueId) {
        final User owner = findOwner(ownerEmail);

        final String oldObjectKey = venuePhotoUpdater.clearPhotoKey(venueId, owner.getId());
        if (oldObjectKey != null) {
            safePhotoDelete(oldObjectKey);
        }

        return venueRepository.findByIdAndOwnerId(venueId, owner.getId())
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private void safePhotoDelete(final String objectKey) {
        try {
            venuePhotoStorage.delete(objectKey);
        } catch (RuntimeException e) {
            log.warn("Failed to delete photo object: {}", objectKey, e);
        }
    }

    private User findOwner(final String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException(ownerEmail));
    }
}
