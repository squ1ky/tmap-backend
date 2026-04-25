package ru.tbank.tmap.venue;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.VenuesPublicApi;
import org.openapitools.model.VenuePublicResponse;
import org.openapitools.model.VenueSearchResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.search.VenueSearchService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class VenueController implements VenuesPublicApi {

    private final PublicVenueService publicVenueService;
    private final VenueSearchService venueSearchService;

    @Override
    public ResponseEntity<List<VenuePublicResponse>> getVenuesInViewport(
            final Double swLat,
            final Double swLng,
            final Double neLat,
            final Double neLng,
            final List<String> category) {
        return ResponseEntity.ok(publicVenueService.getVenuesInViewport(
                new BoundingBox(swLat, swLng, neLat, neLng),
                toCategories(category)));
    }

    @Override
    public ResponseEntity<VenuePublicResponse> getVenueById(final UUID id) {
        return publicVenueService.getVenueById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
    }

    @Override
    public ResponseEntity<List<VenueSearchResultResponse>> searchVenues(final String q) {
        return ResponseEntity.ok(venueSearchService.searchByName(q));
    }

    private List<VenueCategory> toCategories(final List<String> category) {
        if (category == null) {
            return List.of();
        }
        return category.stream()
                .map(VenueCategory::fromString)
                .toList();
    }
}
