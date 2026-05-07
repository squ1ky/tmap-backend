package ru.tbank.tmap.venue.application.service.business;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;
import ru.tbank.tmap.venue.application.command.VenueUpdateCommand;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;
import ru.tbank.tmap.venue.application.command.VenueCreateCommand;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.application.service.VenueH3Resolver;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessVenueService {

    private final UserAccountFacade userAccountFacade;
    private final VenueRepository venueRepository;
    private final VenuePendingUpdateRepository venuePendingUpdateRepository;
    private final VenueH3Resolver venueH3Resolver;

    @Transactional
    public VenueDetails createVenue(final UUID ownerId, final VenueCreateCommand command) {
        verifyOwnerExists(ownerId);

        final long h3Res9 = venueH3Resolver.toH3Res9(command.location());

        final Venue venue = Venue.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name(command.name())
                .address(command.address())
                .location(command.location())
                .h3Res9(h3Res9)
                .category(command.category())
                .description(command.description())
                .dishOfDay(command.dishOfDay())
                .music(command.music())
                .status(VenueStatus.PENDING)
                .build();

        venueRepository.save(venue);

        return new VenueDetails(venue, null);
    }

    public List<VenueDetails> getMyVenues(final UUID ownerId) {
        final List<Venue> venues = venueRepository.findByOwnerIdOrderByNameAscIdAsc(ownerId);
        final Map<UUID, VenuePendingUpdate> pendingUpdatesByVenueId = venuePendingUpdateRepository.findByVenueIdIn(
                        venues.stream().map(Venue::getId).toList())
                .stream()
                .collect(Collectors.toMap(VenuePendingUpdate::getVenueId, pending -> pending));
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
                .orElseThrow(() -> new VenueNotFoundException(venueId));
        final long h3Res9 = venueH3Resolver.resolveUpdatedH3Res9(venue, command.location());

        if (venue.getStatus() == VenueStatus.ACTIVE) {
            final VenuePendingUpdate pendingUpdate = venuePendingUpdateRepository.findByVenueId(venueId)
                    .map(existing -> {
                        existing.applyUpdate(command, h3Res9);
                        return existing;
                    })
                    .orElseGet(() -> VenuePendingUpdate.create(venue, command, h3Res9));
            final VenuePendingUpdate savedPendingUpdate = venuePendingUpdateRepository.save(pendingUpdate);
            return new VenueDetails(venue, savedPendingUpdate);
        }

        venue.applyUpdate(command, h3Res9);
        if (venue.getStatus() == VenueStatus.REJECTED) {
            venue.setStatus(VenueStatus.PENDING);
            venue.setRejectReason(null);
        }
        return new VenueDetails(venueRepository.save(venue), null);
    }

    private void verifyOwnerExists(final UUID ownerId) {
        userAccountFacade.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId.toString()));
    }
}
