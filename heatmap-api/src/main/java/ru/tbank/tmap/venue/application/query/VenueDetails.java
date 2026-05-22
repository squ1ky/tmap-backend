package ru.tbank.tmap.venue.application.query;

import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueContent;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;

import java.time.OffsetDateTime;

public record VenueDetails(Venue venue, VenuePendingUpdate pendingUpdate) {

    public VenueContent displayContent() {
        return pendingUpdate != null ? pendingUpdate.getContent() : venue.getContent();
    }

    public String displayPhotoObjectKey() {
        if (pendingUpdate != null && pendingUpdate.getPendingPhotoObjectKey() != null) {
            return pendingUpdate.getPendingPhotoObjectKey();
        }
        return venue.getPhotoObjectKey();
    }

    public VenueStatus displayStatus() {
        return pendingUpdate != null ? pendingUpdate.getStatus() : venue.getStatus();
    }

    public String displayRejectReason() {
        return pendingUpdate != null ? pendingUpdate.getRejectReason() : venue.getRejectReason();
    }

    public OffsetDateTime displayUpdatedAt() {
        return pendingUpdate != null ? pendingUpdate.getUpdatedAt() : venue.getUpdatedAt();
    }
}
