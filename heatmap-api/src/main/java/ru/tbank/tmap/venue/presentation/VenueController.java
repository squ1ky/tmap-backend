package ru.tbank.tmap.venue.presentation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.VenuesPublicApi;
import org.openapitools.model.VenuePublicResponse;
import org.openapitools.model.VenueSearchResultResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tbank.tmap.shared.geo.BoundingBox;
import ru.tbank.tmap.venue.application.query.VenuePromoProjection;
import ru.tbank.tmap.venue.application.query.VenueProjection;
import ru.tbank.tmap.venue.application.service.VenueQueryService;
import ru.tbank.tmap.venue.application.service.VenueSearchService;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class VenueController implements VenuesPublicApi {

    private final VenueQueryService publicVenueService;
    private final VenueSearchService venueSearchService;
    private final VenueMapper venuePublicMapper;

    @Override
    public ResponseEntity<List<VenuePublicResponse>> getVenuesInViewport(
            final Double swLat,
            final Double swLng,
            final Double neLat,
            final Double neLng,
            final List<String> category) {
        final List<VenueProjection> venues = publicVenueService.getVenuesInViewport(
                new BoundingBox(swLat, swLng, neLat, neLng),
                toCategories(category)
        );
        final Map<UUID, List<VenuePromoProjection>> promotionsByVenueId = publicVenueService.getVenuePromosByVenueIds(
                venues.stream().map(VenueProjection::id).toList()
        );
        return ResponseEntity.ok(venues.stream()
                .map(venue -> venuePublicMapper.toResponse(
                        venue,
                        promotionsByVenueId.getOrDefault(venue.id(), List.of())
                ))
                .toList());
    }

    @Override
    public ResponseEntity<VenuePublicResponse> getVenueById(final UUID id) {
        return publicVenueService.getVenueById(id)
                .map(venue -> venuePublicMapper.toResponse(venue, publicVenueService.getVenuePromos(venue.id())))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new VenueNotFoundException(id));
    }

    @Override
    public ResponseEntity<List<VenueSearchResultResponse>> searchVenues(final String q) {
        return ResponseEntity.ok(venueSearchService.searchByName(q).stream()
                .map(venuePublicMapper::toSearchResponse)
                .toList());
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
