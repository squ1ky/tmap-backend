package ru.tbank.tmap.venue.application.port;

import java.io.InputStream;
import java.util.UUID;

public interface VenuePhotoStorage {

    /**
     * Stores a venue photo in the underlying object storage.
     *
     * @param venueId     id of the venue this photo belongs to (used to build the object key)
     * @param stream      photo binary content
     * @param size        size of the photo in bytes; must be non-negative
     * @param contentType MIME type of the photo (e.g. "image/jpeg")
     * @param extension   file extension to use in the generated object key (e.g. "jpg")
     * @return the generated object key under which the photo was stored
     */
    String upload(UUID venueId, InputStream stream, long size, String contentType, String extension);

    /**
     * Removes the object identified by the given key. No-op if the object does not exist.
     */
    void delete(String objectKey);
}
