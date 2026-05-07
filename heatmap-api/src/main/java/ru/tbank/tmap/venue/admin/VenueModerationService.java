package ru.tbank.tmap.venue.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.venue.application.VenueDetails;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.exception.VenueModerationStateException;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Service
@Transactional(readOnly = true)
public class VenueModerationService {

    private final VenueRepository venueRepository;
    private final VenuePendingUpdateRepository venuePendingUpdateRepository;

    public VenueModerationService(
            final VenueRepository venueRepository,
            final VenuePendingUpdateRepository venuePendingUpdateRepository
    ) {
        this.venueRepository = venueRepository;
        this.venuePendingUpdateRepository = venuePendingUpdateRepository;
    }

    public Page<VenueDetails> getAdminVenues(
            final VenueStatus status,
            final int page,
            final int size
    ) {
        final Pageable pageable = PageRequest.of(page, size);
        if (status == VenueStatus.PENDING_UPDATE) {
            return venuePendingUpdateRepository.findByStatus(VenueStatus.PENDING_UPDATE, pageable)
                    .map(pendingUpdate -> new VenueDetails(pendingUpdate.getVenue(), pendingUpdate));
        }
        return venueRepository.findByStatus(status, pageable)
                .map(venue -> new VenueDetails(venue, null));
    }

    public Optional<VenueDetails> getAdminVenueById(final UUID id) {
        return venueRepository.findById(id)
                .map(venue -> new VenueDetails(
                        venue,
                        venuePendingUpdateRepository.findByVenueId(id).orElse(null)
                ));
    }

    @Transactional
    public VenueDetails verifyAdminVenue(final UUID id) {
        final VenuePendingUpdate pendingUpdate = venuePendingUpdateRepository.findByVenueId(id).orElse(null);
        if (pendingUpdate != null) {
            if (pendingUpdate.getStatus() != VenueStatus.PENDING_UPDATE) {
                throw new VenueModerationStateException(id, pendingUpdate.getStatus());
            }
            final Venue venue = pendingUpdate.getVenue();
            venue.applyPendingUpdate(pendingUpdate);
            venue.setStatus(VenueStatus.ACTIVE);
            venue.setRejectReason(null);
            final Venue savedVenue = venueRepository.save(venue);
            venuePendingUpdateRepository.delete(pendingUpdate);
            return new VenueDetails(savedVenue, null);
        }

        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setRejectReason(null);
        return new VenueDetails(venueRepository.save(venue), null);
    }

    @Transactional
    public VenueDetails rejectAdminVenue(final UUID id, final String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reject reason must not be blank");
        }

        final VenuePendingUpdate pendingUpdate = venuePendingUpdateRepository.findByVenueId(id).orElse(null);
        if (pendingUpdate != null) {
            if (pendingUpdate.getStatus() != VenueStatus.PENDING_UPDATE) {
                throw new VenueModerationStateException(id, pendingUpdate.getStatus());
            }
            pendingUpdate.setStatus(VenueStatus.REJECTED);
            pendingUpdate.setRejectReason(reason.trim());
            final VenuePendingUpdate savedPendingUpdate = venuePendingUpdateRepository.save(pendingUpdate);
            return new VenueDetails(savedPendingUpdate.getVenue(), savedPendingUpdate);
        }

        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.REJECTED);
        venue.setRejectReason(reason.trim());
        return new VenueDetails(venueRepository.save(venue), null);
    }

    private Venue findPendingVenue(final UUID id) {
        final Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException(id));
        if (venue.getStatus() != VenueStatus.PENDING) {
            throw new VenueModerationStateException(id, venue.getStatus());
        }
        return venue;
    }

}
