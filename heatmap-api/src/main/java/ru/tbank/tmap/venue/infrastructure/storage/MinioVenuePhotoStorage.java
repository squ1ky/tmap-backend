package ru.tbank.tmap.venue.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.infrastructure.minio.MinioObjectStorage;
import ru.tbank.tmap.venue.application.port.VenuePhotoStorage;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MinioVenuePhotoStorage implements VenuePhotoStorage {

    private final MinioObjectStorage minioObjectStorage;

    @Override
    public String upload(
            final UUID venueId,
            final InputStream stream,
            final long size,
            final String contentType,
            final String extension
    ) {
        final String objectKey = buildObjectKey(venueId, extension);
        minioObjectStorage.putObject(objectKey, stream, size, contentType);
        return objectKey;
    }

    @Override
    public void delete(String objectKey) {
        minioObjectStorage.deleteObject(objectKey);
    }

    private String buildObjectKey(final UUID venueId, final String extension) {
        return "venues/" + venueId + "/" + UUID.randomUUID() + "." + extension;
    }
}
