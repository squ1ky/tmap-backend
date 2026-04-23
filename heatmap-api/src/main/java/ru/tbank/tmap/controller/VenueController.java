package ru.tbank.tmap.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.VenuesApi;
import org.openapitools.model.VenuePublicResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.tbank.tmap.domain.geo.BoundingBox;
import ru.tbank.tmap.domain.venue.VenueCategory;
import ru.tbank.tmap.service.VenueService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VenueController implements VenuesApi {

    private final VenueService venueService;

    @Override
    public ResponseEntity<List<VenuePublicResponse>> getVenuesInViewport(
            final Double swLat,
            final Double swLng,
            final Double neLat,
            final Double neLng,
            final List<String> category
    ) {
        return ResponseEntity.ok(venueService.getVenuesInViewport(
                new BoundingBox(swLat, swLng, neLat, neLng),
                toCategories(category)
        ));
    }

    @Override
    public ResponseEntity<VenuePublicResponse> getVenueById(final UUID id) {
        return venueService.getVenueById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
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
