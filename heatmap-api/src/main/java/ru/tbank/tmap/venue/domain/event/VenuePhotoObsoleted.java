package ru.tbank.tmap.venue.domain.event;

import java.util.Objects;

/**
 * Published when a venue's photo object in storage is no longer referenced
 * by any aggregate and should be deleted.
 * <p>
 * Handled asynchronously after the transaction commits.
 */
public record VenuePhotoObsoleted(String objectKey) {
    public VenuePhotoObsoleted {
        Objects.requireNonNull(objectKey, "objectKey");
    }
}
