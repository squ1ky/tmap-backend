package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.BusinessOwnerApi;
import org.openapitools.model.VenueCreateRequest;
import org.openapitools.model.VenueOwnerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BusinessVenueController implements BusinessOwnerApi {

    private final BusinessVenueService businessVenueService;
    private final BusinessVenueMapper businessVenueMapper;
    private final VenueOwnerMapper venueOwnerMapper;

    @Override
    public ResponseEntity<VenueOwnerResponse> createVenue(final VenueCreateRequest venueCreateRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueOwnerMapper.toResponse(
                        businessVenueService.createVenue(
                                currentUserEmail(),
                                businessVenueMapper.toCommand(venueCreateRequest)
                        )
                ));
    }

    @Override
    public ResponseEntity<List<VenueOwnerResponse>> getMyVenues() {
        return ResponseEntity.ok(businessVenueService.getMyVenues(currentUserEmail()).stream()
                .map(venueOwnerMapper::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<VenueOwnerResponse> getMyVenueById(final UUID id) {
        return businessVenueService.getMyVenueById(currentUserEmail(), id)
                .map(venueOwnerMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new VenueNotFoundException(id));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
