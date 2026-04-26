package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.VenueCreateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
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
    private final H3IndexService h3IndexService;
    private final BusinessVenueMapper businessVenueMapper;

    @Transactional
    public Venue createVenue(final String ownerEmail, final VenueCreateRequest request) {
        final User owner = findOwner(ownerEmail);
        final GeoPoint location = GeoPoint.of(request.getLat(), request.getLng());
        final long h3Res9 = h3IndexService.toH3(location.getLat(), location.getLng(), H3Resolution.RES_9);
        return venueRepository.save(businessVenueMapper.toEntity(request, owner, location, h3Res9));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
