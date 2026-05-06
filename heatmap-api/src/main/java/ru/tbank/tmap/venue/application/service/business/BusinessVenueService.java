package ru.tbank.tmap.venue.application.service.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;
import ru.tbank.tmap.venue.application.command.VenueCreateCommand;
import ru.tbank.tmap.venue.domain.Venue;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessVenueService {

    private final VenueRepository venueRepository;
    private final UserAccountFacade userAccountFacade;
    private final H3IndexService h3IndexService;

    @Transactional
    public Venue createVenue(final UUID ownerId, final VenueCreateCommand command) {
        verifyOwnerExists(ownerId);

        final long h3Res9 = h3IndexService.toH3(
                command.location().getLat(),
                command.location().getLng(),
                H3Resolution.RES_9
        );

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

        return venueRepository.save(venue);
    }

    public List<Venue> getMyVenues(final UUID ownerId) {
        return venueRepository.findByOwnerIdOrderByNameAscIdAsc(ownerId);
    }

    public Optional<Venue> getMyVenueById(final UUID ownerId, final UUID venueId) {
        return venueRepository.findByIdAndOwnerId(venueId, ownerId);
    }

    private void verifyOwnerExists(final UUID ownerId) {
        userAccountFacade.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId.toString()));
    }
}
