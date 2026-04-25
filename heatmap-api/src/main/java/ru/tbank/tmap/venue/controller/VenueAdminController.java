package ru.tbank.tmap.venue.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.AdminApi;
import org.openapitools.model.AdminModerationDecision;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.venue.VenueService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VenueAdminController implements AdminApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<AdminVenueModerationPage> getAdminVenues(
            final VenueModerationStatus status,
            final Integer page,
            final Integer size
    ) {
        return ResponseEntity.ok(venueService.getAdminVenues(status, page, size));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> getAdminVenueById(final UUID id) {
        return venueService.getAdminVenueById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> verifyAdminVenue(final UUID id) {
        return ResponseEntity.ok(venueService.verifyAdminVenue(id));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> rejectAdminVenue(
            final UUID id,
            final AdminModerationDecision adminModerationDecision
    ) {
        return ResponseEntity.ok(venueService.rejectAdminVenue(id, adminModerationDecision));
    }
}
