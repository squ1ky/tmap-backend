package ru.tbank.tmap.venue.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.exception.VenueModerationStateException;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;

@Service
@Transactional(readOnly = true)
public class VenueModerationService {

    private final VenueRepository venueRepository;

    public VenueModerationService(final VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Page<Venue> getAdminVenues(
            final VenueStatus status,
            final int page,
            final int size
    ) {
        final Pageable pageable = PageRequest.of(page, size);
        return venueRepository.findByStatus(status, pageable);
    }

    public Optional<Venue> getAdminVenueById(final UUID id) {
        return venueRepository.findById(id);
    }

    @Transactional
    public Venue verifyAdminVenue(final UUID id) {
        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setRejectReason(null);
        return venueRepository.save(venue);
    }

    @Transactional
    public Venue rejectAdminVenue(final UUID id, final String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reject reason must not be blank");
        }

        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.REJECTED);
        venue.setRejectReason(reason.trim());
        return venueRepository.save(venue);
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
