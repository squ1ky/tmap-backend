package ru.tbank.tmap.venue.application.service.admin;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVenueService {

    private final VenueRepository venueRepository;
    private final VenuePendingUpdateRepository venuePendingUpdateRepository;

    public Page<VenueDetails> getVenues(
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

    public Optional<VenueDetails> getVenueById(final UUID id) {
        return venueRepository.findById(id)
                .map(venue -> new VenueDetails(
                        venue,
                        venuePendingUpdateRepository.findByVenueId(id).orElse(null)
                ));
    }

    @Transactional
    public VenueDetails verifyAdminVenue(final UUID id) {
        return venuePendingUpdateRepository.findByVenueId(id)
                .map(this::approvePendingUpdate)
                .orElseGet(() -> approveNewVenue(id));
    }

    @Transactional
    public VenueDetails rejectAdminVenue(final UUID id, final String reason) {
        return venuePendingUpdateRepository.findByVenueId(id)
                .map(pendingUpdate -> rejectPendingUpdate(pendingUpdate, reason))
                .orElseGet(() -> rejectNewVenue(id, reason));
    }

    private VenueDetails approvePendingUpdate(VenuePendingUpdate pendingUpdate) {
        Venue venue = pendingUpdate.getVenue();
        venue.approveWithUpdate(pendingUpdate);
        Venue saved = venueRepository.save(venue);
        venuePendingUpdateRepository.deleteById(pendingUpdate.getVenueId());
        return new VenueDetails(saved, null);
    }

    private VenueDetails approveNewVenue(UUID id) {
        Venue venue = findVenue(id);
        venue.approve();
        Venue saved = venueRepository.save(venue);
        return new VenueDetails(saved, null);
    }

    private VenueDetails rejectPendingUpdate(VenuePendingUpdate pendingUpdate, String reason) {
        pendingUpdate.reject(reason);
        VenuePendingUpdate saved = venuePendingUpdateRepository.save(pendingUpdate);
        return new VenueDetails(saved.getVenue(), saved);
    }

    private VenueDetails rejectNewVenue(UUID id, String reason) {
        Venue venue = findVenue(id);
        venue.reject(reason);
        return new VenueDetails(venueRepository.save(venue), null);
    }

    private Venue findVenue(UUID id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException(id));
    }
}
