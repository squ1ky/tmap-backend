package ru.tbank.tmap.venue.application.service.business;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;
import ru.tbank.tmap.venue.application.command.VenueUpdateCommand;
import ru.tbank.tmap.venue.domain.VenueContent;
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
        final VenueContent content = command.toContent(h3Res9);

        final Venue venue = Venue.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .content(content)
                .status(VenueStatus.PENDING)
                .build();

        venueRepository.save(venue);

        return new VenueDetails(venue, null);
    }

    public List<VenueDetails> getMyVenues(final UUID ownerId) {
        final List<Venue> venues = venueRepository.findByOwnerIdOrderByNameAscIdAsc(ownerId);
        final Map<UUID, VenuePendingUpdate> pendingByVenueId = loadPendingUpdatesFor(venues);

        return venues.stream()
                .map(venue -> new VenueDetails(venue, pendingByVenueId.get(venue.getId())))
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
        final Venue venue = findOwnedVenue(ownerId, venueId);
        final long h3Res9 = venueH3Resolver.resolveUpdatedH3Res9(venue, command.location());

        VenueContent content = command.toContent(h3Res9);

        if (venue.getStatus() == VenueStatus.ACTIVE) {
            return upsertPendingUpdate(venue, content);
        }

        return editVenue(venue, content);
    }

    private VenueDetails upsertPendingUpdate(final Venue venue, final VenueContent content) {
        final VenuePendingUpdate pendingUpdate = venuePendingUpdateRepository.findByVenueId(venue.getId())
                .map(existing -> {
                    existing.applyContent(content);
                    return existing;
                })
                .orElseGet(() -> VenuePendingUpdate.create(venue, content));

        final VenuePendingUpdate saved = venuePendingUpdateRepository.save(pendingUpdate);

        return new VenueDetails(venue, saved);
    }

    private VenueDetails editVenue(final Venue venue, final VenueContent content) {
        venue.applyContent(content);
        if (venue.getStatus() == VenueStatus.REJECTED) {
            venue.setStatus(VenueStatus.PENDING);
            venue.setRejectReason(null);
        }

        Venue saved = venueRepository.save(venue);
        return new VenueDetails(saved, null);
    }

    private Venue findOwnedVenue(final UUID userId, final UUID venueId) {
        return venueRepository.findByIdAndOwnerId(venueId, userId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private Map<UUID, VenuePendingUpdate> loadPendingUpdatesFor(final List<Venue> venues) {
        final List<UUID> venueIds = venues.stream()
                .map(Venue::getId)
                .toList();
        return venuePendingUpdateRepository.findByVenueIdIn(venueIds).stream()
                .collect(Collectors.toMap(VenuePendingUpdate::getVenueId, Function.identity()));
    }

    private void verifyOwnerExists(final UUID ownerId) {
        userAccountFacade.findById(ownerId)
                .orElseThrow(() -> UserNotFoundException.byId(ownerId));
    }
}
