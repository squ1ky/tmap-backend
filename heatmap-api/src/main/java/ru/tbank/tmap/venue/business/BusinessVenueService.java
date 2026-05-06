package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;
import ru.tbank.tmap.user.domain.UserRepository;
import ru.tbank.tmap.venue.application.VenueDetails;
import ru.tbank.tmap.venue.application.VenueH3Resolver;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessVenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final VenueH3Resolver venueH3Resolver;
    private final BusinessVenueMapper businessVenueMapper;
    private final VenuePendingUpdateRepository venuePendingUpdateRepository;

    @Transactional
    public VenueDetails createVenue(final UUID ownerId, final VenueCreateCommand command) {
        final User owner = findOwner(ownerId);
        final long h3Res9 = venueH3Resolver.toH3Res9(command.location());
        final Venue venue = venueRepository.save(businessVenueMapper.toEntity(command, owner, h3Res9));
        return new VenueDetails(venue, null);
    }

    public List<VenueDetails> getMyVenues(final UUID ownerId) {
        final List<Venue> venues = venueRepository.findByOwnerIdOrderByNameAscIdAsc(ownerId);
        final Map<UUID, VenuePendingUpdate> pendingUpdatesByVenueId = venuePendingUpdateRepository.findByVenueIdIn(
                        venues.stream().map(Venue::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(VenuePendingUpdate::getVenueId, pending -> pending));
        return venues.stream()
                .map(venue -> new VenueDetails(venue, pendingUpdatesByVenueId.get(venue.getId())))
                .toList();
    }

    public Optional<VenueDetails> getMyVenueById(final UUID ownerId, final UUID venueId) {
        return venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .map(venue -> new VenueDetails(
                        venue,
                        venuePendingUpdateRepository.findByVenueId(venueId).orElse(null)
                ));
    }

    @Transactional
    public VenueDetails updateVenue(
            final UUID ownerId,
            final UUID venueId,
            final VenueUpdateCommand command
    ) {
        final Venue venue = venueRepository.findByIdAndOwnerId(venueId, ownerId)
                .orElseThrow(() -> new ru.tbank.tmap.venue.exception.VenueNotFoundException(venueId));
        final long h3Res9 = venueH3Resolver.resolveH3Res9(venue, command.location());

        if (venue.getStatus() == VenueStatus.ACTIVE) {
            final VenuePendingUpdate pendingUpdate = venuePendingUpdateRepository.findByVenueId(venueId)
                    .map(existing -> {
                        businessVenueMapper.updatePendingUpdate(existing, command, h3Res9);
                        return existing;
                    })
                    .orElseGet(() -> businessVenueMapper.toPendingUpdate(venue, command, h3Res9));
            final VenuePendingUpdate savedPendingUpdate = venuePendingUpdateRepository.save(pendingUpdate);
            return new VenueDetails(venue, savedPendingUpdate);
        }

        businessVenueMapper.applyPayloadToVenue(venue, command, h3Res9);
        if (venue.getStatus() == VenueStatus.REJECTED) {
            venue.setStatus(VenueStatus.PENDING);
            venue.setRejectReason(null);
        }
        return new VenueDetails(venueRepository.save(venue), null);
    }

    private User findOwner(final UUID ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId.toString()));
    }
}
