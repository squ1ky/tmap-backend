package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;
import ru.tbank.tmap.user.domain.UserRepository;
import ru.tbank.tmap.venue.domain.Venue;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessVenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final H3IndexService h3IndexService;
    private final BusinessVenueMapper businessVenueMapper;

    @Transactional
    public Venue createVenue(final UUID ownerId, final VenueCreateCommand command) {
        final User owner = findOwner(ownerId);
        final long h3Res9 = h3IndexService.toH3(
                command.location().getLat(),
                command.location().getLng(),
                H3Resolution.RES_9
        );
        return venueRepository.save(businessVenueMapper.toEntity(command, owner, h3Res9));
    }

    public List<Venue> getMyVenues(final UUID ownerId) {
        return venueRepository.findByOwnerIdOrderByNameAscIdAsc(ownerId);
    }

    public Optional<Venue> getMyVenueById(final UUID ownerId, final UUID venueId) {
        return venueRepository.findByIdAndOwnerId(venueId, ownerId);
    }

    private User findOwner(final UUID ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId.toString()));
    }
}
