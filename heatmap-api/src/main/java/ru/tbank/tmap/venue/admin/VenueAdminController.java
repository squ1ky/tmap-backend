package ru.tbank.tmap.venue.admin;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.AdminVenuesApi;
import org.openapitools.model.AdminModerationDecision;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VenueAdminController implements AdminVenuesApi {

    private final VenueModerationService venueModerationService;

    @Override
    public ResponseEntity<AdminVenueModerationPage> getAdminVenues(
            final VenueModerationStatus status,
            final Integer page,
            final Integer size
    ) {
        return ResponseEntity.ok(venueModerationService.getAdminVenues(status, page, size));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> getAdminVenueById(final UUID id) {
        return venueModerationService.getAdminVenueById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> verifyAdminVenue(final UUID id) {
        return ResponseEntity.ok(venueModerationService.verifyAdminVenue(id));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> rejectAdminVenue(
            final UUID id,
            final AdminModerationDecision adminModerationDecision
    ) {
        return ResponseEntity.ok(venueModerationService.rejectAdminVenue(id, adminModerationDecision));
    }
}
