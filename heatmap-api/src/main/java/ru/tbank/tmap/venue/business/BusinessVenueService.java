package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserNotFoundException;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCreateCommand;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessVenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final H3IndexService h3IndexService;
    private final BusinessVenueMapper businessVenueMapper;

    @Transactional
    public Venue createVenue(final String ownerEmail, final VenueCreateCommand command) {
        final User owner = findOwner(ownerEmail);
        final long h3Res9 = h3IndexService.toH3(
                command.location().getLat(),
                command.location().getLng(),
                H3Resolution.RES_9
        );
        return venueRepository.save(businessVenueMapper.toEntity(command, owner, h3Res9));
    }

    public List<Venue> getMyVenues(final String ownerEmail) {
        final User owner = findOwner(ownerEmail);
        return venueRepository.findByOwnerIdOrderByNameAscIdAsc(owner.getId());
    }

    public Optional<Venue> getMyVenueById(final String ownerEmail, final UUID venueId) {
        final User owner = findOwner(ownerEmail);
        return venueRepository.findByIdAndOwnerId(venueId, owner.getId());
    }

    private User findOwner(final String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException(ownerEmail));
    }
}
