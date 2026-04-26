package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.user.UserRepository;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.repository.VenueRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessVenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
