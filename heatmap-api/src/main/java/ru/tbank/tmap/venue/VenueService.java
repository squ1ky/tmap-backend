package ru.tbank.tmap.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openapitools.model.AdminModerationDecision;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenuePublicResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.repository.VenueQueryRepository;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Service
@Transactional(readOnly = true)
public class VenueService {

    private static final int MAX_TOTAL_ELEMENTS = Integer.MAX_VALUE;

    private final VenueRepository venueRepository;
    private final VenueQueryRepository venueQueryRepository;
    private final VenuePublicMapper venuePublicMapper;

    public VenueService(
            final VenueRepository venueRepository,
            final VenueQueryRepository venueQueryRepository,
            final VenuePublicMapper venuePublicMapper
    ) {
        this.venueRepository = venueRepository;
        this.venueQueryRepository = venueQueryRepository;
        this.venuePublicMapper = venuePublicMapper;
    }

    public List<VenuePublicResponse> getVenuesInViewport(
            final BoundingBox boundingBox,
            final List<VenueCategory> categories
    ) {
        return venueQueryRepository.findActiveInViewport(boundingBox, categories).stream()
                .map(venuePublicMapper::toResponse)
                .toList();
    }

    public Optional<VenuePublicResponse> getVenueById(final UUID id) {
        return venueQueryRepository.findActiveById(id)
                .map(venuePublicMapper::toResponse);
    }

    public AdminVenueModerationPage getAdminVenues(
            final VenueModerationStatus status,
            final int page,
            final int size
    ) {
        final VenueStatus venueStatus = toVenueStatus(status == null ? VenueModerationStatus.PENDING : status);
        final Pageable pageable = PageRequest.of(page, size);
        final Page<Venue> venues = venueRepository.findByStatus(venueStatus, pageable);

        return new AdminVenueModerationPage()
                .items(venues.stream()
                        .map(venuePublicMapper::toAdminModerationResponse)
                        .toList())
                .page(venues.getNumber())
                .size(venues.getSize())
                .totalPages(venues.getTotalPages())
                .totalElements(toIntTotalElements(venues.getTotalElements()));
    }

    public Optional<AdminVenueModerationResponse> getAdminVenueById(final UUID id) {
        return venueRepository.findById(id)
                .map(venuePublicMapper::toAdminModerationResponse);
    }

    @Transactional
    public AdminVenueModerationResponse verifyAdminVenue(final UUID id) {
        final Venue venue = findPendingVenue(id);
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setRejectReason(null);
        return venuePublicMapper.toAdminModerationResponse(venueRepository.save(venue));
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
        return venuePublicMapper.toAdminModerationResponse(venueRepository.save(venue));
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

    private int toIntTotalElements(final long totalElements) {
        if (totalElements > MAX_TOTAL_ELEMENTS) {
            return MAX_TOTAL_ELEMENTS;
        }
        return (int) totalElements;
    }
}
