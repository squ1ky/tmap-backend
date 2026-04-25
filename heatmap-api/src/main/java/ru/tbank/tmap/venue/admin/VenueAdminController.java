package ru.tbank.tmap.venue.admin;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.AdminVenuesApi;
import org.openapitools.model.AdminModerationDecision;
import org.openapitools.model.AdminVenueModerationPage;
import org.openapitools.model.AdminVenueModerationResponse;
import org.openapitools.model.VenueModerationStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.venue.domain.VenueNotFoundException;
import ru.tbank.tmap.venue.domain.VenueStatus;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VenueAdminController implements AdminVenuesApi {

    private final VenueModerationService venueModerationService;
    private final VenueModerationMapper venueModerationMapper;

    @Override
    public ResponseEntity<AdminVenueModerationPage> getAdminVenues(
            final VenueModerationStatus status,
            final Integer page,
            final Integer size
    ) {
        return ResponseEntity.ok(venueModerationMapper.toPage(
                venueModerationService.getAdminVenues(toVenueStatus(status), page, size)
        ));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> getAdminVenueById(final UUID id) {
        return venueModerationService.getAdminVenueById(id)
                .map(venueModerationMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new VenueNotFoundException(id));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> verifyAdminVenue(final UUID id) {
        return ResponseEntity.ok(venueModerationMapper.toResponse(venueModerationService.verifyAdminVenue(id)));
    }

    @Override
    public ResponseEntity<AdminVenueModerationResponse> rejectAdminVenue(
            final UUID id,
            final AdminModerationDecision adminModerationDecision
    ) {
        final String reason = adminModerationDecision == null ? null : adminModerationDecision.getReason();
        return ResponseEntity.ok(venueModerationMapper.toResponse(venueModerationService.rejectAdminVenue(id, reason)));
    }

    private VenueStatus toVenueStatus(final VenueModerationStatus status) {
        return VenueStatus.valueOf((status == null ? VenueModerationStatus.PENDING : status).getValue());
    }
}
