package ru.tbank.tmap.venue.business;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.BusinessOwnerApi;
import org.openapitools.model.VenueCreateRequest;
import org.openapitools.model.VenueOwnerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.tbank.tmap.shared.utils.SecurityUtils;
import ru.tbank.tmap.venue.business.photo.BusinessVenuePhotoService;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.exception.VenueNotFoundException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BusinessVenueController implements BusinessOwnerApi {

    private final BusinessVenueService businessVenueService;
    private final BusinessVenuePhotoService businessVenuePhotoService;
    private final BusinessVenueMapper businessVenueMapper;
    private final VenueOwnerMapper venueOwnerMapper;

    @Override
    public ResponseEntity<VenueOwnerResponse> createVenue(final VenueCreateRequest venueCreateRequest) {
        final String ownerEmail = SecurityUtils.currentUserEmail();
        final VenueCreateCommand command = businessVenueMapper.toCommand(venueCreateRequest);
        final Venue venue = businessVenueService.createVenue(ownerEmail, command);
        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<VenueOwnerResponse>> getMyVenues() {
        final String ownerEmail = SecurityUtils.currentUserEmail();
        final List<Venue> venues = businessVenueService.getMyVenues(ownerEmail);
        return ResponseEntity.ok(venues.stream()
                .map(venueOwnerMapper::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<VenueOwnerResponse> getMyVenueById(final UUID id) {
        final String ownerEmail = SecurityUtils.currentUserEmail();
        final Venue venue = businessVenueService.getMyVenueById(ownerEmail, id)
                .orElseThrow(() -> new VenueNotFoundException(id));
        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<VenueOwnerResponse> uploadVenuePhoto(UUID id, MultipartFile file) {
        final String ownerEmail = SecurityUtils.currentUserEmail();
        final Venue venue = businessVenuePhotoService.uploadVenuePhoto(ownerEmail, id, file);
        return ResponseEntity.ok(venueOwnerMapper.toResponse(venue));
    }

    @Override
    public ResponseEntity<VenueOwnerResponse> deleteVenuePhoto(UUID id) {
        final String ownerEmail = SecurityUtils.currentUserEmail();
        final Venue venue = businessVenuePhotoService.deleteVenuePhoto(ownerEmail, id);
        return ResponseEntity.ok(venueOwnerMapper.toResponse(venue));
    }
}
