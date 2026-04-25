package ru.tbank.tmap.venue.admin;

import java.util.Optional;
import java.util.UUID;
import org.openapitools.model.AdminModerationDecision;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Service
@Transactional(readOnly = true)
public class VenueModerationService {

    private final VenueRepository venueRepository;
    private final VenueModerationMapper venueModerationMapper;

    public VenueModerationService(
            final VenueRepository venueRepository,
            final VenueModerationMapper venueModerationMapper
    ) {
        this.venueRepository = venueRepository;
        this.venueModerationMapper = venueModerationMapper;
    }

    public AdminVenueModerationPage getAdminVenues(
            final VenueModerationStatus status,
            final int page,
            final int size
    ) {
        final VenueStatus venueStatus = toVenueStatus(status == null ? VenueModerationStatus.PENDING : status);
        final Pageable pageable = PageRequest.of(page, size);
        final Page<Venue> venues = venueRepository.findByStatus(venueStatus, pageable);

        return venueModerationMapper.toPage(venues);
    }

    public Optional<AdminVenueModerationResponse> getAdminVenueById(final UUID id) {
        return venueRepository.findById(id)
                .map(venueModerationMapper::toResponse);
    }

    @Transactional
    public AdminVenueModerationResponse verifyAdminVenue(final UUID id) {
        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setRejectReason(null);
        return venueModerationMapper.toResponse(venueRepository.save(venue));
    }

    @Transactional
    public AdminVenueModerationResponse rejectAdminVenue(final UUID id, final AdminModerationDecision decision) {
        final String reason = decision == null ? null : decision.getReason();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reject reason must not be blank");
        }

        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.REJECTED);
        venue.setRejectReason(reason.trim());
        return venueModerationMapper.toResponse(venueRepository.save(venue));
    }

    private Venue findPendingVenue(final UUID id) {
        final Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        if (venue.getStatus() != VenueStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only PENDING venues can be moderated"
            );
        }
        return venue;
    }

    private VenueStatus toVenueStatus(final VenueModerationStatus status) {
        return VenueStatus.valueOf(status.getValue());
    }
}
