package ru.tbank.tmap.venue.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.venue.business.BusinessVenueMapper;
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
    private final BusinessVenueMapper businessVenueMapper;

    public VenueModerationService(
            final VenueRepository venueRepository,
            final VenuePendingUpdateRepository venuePendingUpdateRepository,
            final BusinessVenueMapper businessVenueMapper
    ) {
        this.venueRepository = venueRepository;
        this.venuePendingUpdateRepository = venuePendingUpdateRepository;
        this.businessVenueMapper = businessVenueMapper;
    }

    public Page<VenueModerationDetails> getAdminVenues(
            final VenueStatus status,
            final int page,
            final int size
    ) {
        final Pageable pageable = PageRequest.of(page, size);
        if (status == VenueStatus.PENDING_UPDATE) {
            return venuePendingUpdateRepository.findByStatus(VenueStatus.PENDING_UPDATE, pageable)
                    .map(pendingUpdate -> new VenueModerationDetails(pendingUpdate.getVenue(), pendingUpdate));
        }
        return venueRepository.findByStatus(status, pageable)
                .map(venue -> new VenueModerationDetails(venue, null));
    }

    public Optional<VenueModerationDetails> getAdminVenueById(final UUID id) {
        return venueRepository.findById(id)
                .map(venue -> new VenueModerationDetails(
                        venue,
                        venuePendingUpdateRepository.findByVenueId(id).orElse(null)
                ));
    }

    @Transactional
    public VenueModerationDetails verifyAdminVenue(final UUID id) {
        final VenuePendingUpdate pendingUpdate = venuePendingUpdateRepository.findByVenueId(id).orElse(null);
        if (pendingUpdate != null) {
            if (pendingUpdate.getStatus() != VenueStatus.PENDING_UPDATE) {
                throw new VenueModerationStateException(id, pendingUpdate.getStatus());
            }
            final Venue venue = pendingUpdate.getVenue();
            businessVenueMapper.applyPendingUpdateToVenue(pendingUpdate, venue);
            venue.setStatus(VenueStatus.ACTIVE);
            venue.setRejectReason(null);
            final Venue savedVenue = venueRepository.save(venue);
            venuePendingUpdateRepository.delete(pendingUpdate);
            return new VenueModerationDetails(savedVenue, null);
        }

        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setRejectReason(null);
        return new VenueModerationDetails(venueRepository.save(venue), null);
    }

    @Transactional
    public VenueModerationDetails rejectAdminVenue(final UUID id, final String reason) {
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
            return new VenueModerationDetails(savedPendingUpdate.getVenue(), savedPendingUpdate);
        }

        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.REJECTED);
        venue.setRejectReason(reason.trim());
        return new VenueModerationDetails(venueRepository.save(venue), null);
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
