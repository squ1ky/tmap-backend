package ru.tbank.tmap.venue.presentation;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.VenuePromoResponse;
import org.openapitools.model.VenuePublicResponse;
import org.openapitools.model.VenueSearchResultResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.venue.application.query.VenuePromoProjection;
import ru.tbank.tmap.venue.application.query.VenueProjection;
import ru.tbank.tmap.venue.application.query.VenueSearchProjection;

@Component
@RequiredArgsConstructor
public class VenueMapper {

    private final MinioUrlBuilder minioUrlBuilder;

    public VenuePublicResponse toResponse(final VenueProjection venue) {
        return toResponse(venue, List.of());
    }

    public VenuePublicResponse toViewportResponse(final VenueProjection venue) {
        return new VenuePublicResponse()
                .id(venue.id())
                .name(venue.name())
                .lat(venue.lat())
                .lng(venue.lng())
                .category(VenuePublicResponse.CategoryEnum.fromValue(
                        venue.category().name().toLowerCase(Locale.ROOT)))
                .peopleNow(0);
    }

    public VenuePublicResponse toResponse(final VenueProjection venue, final List<VenuePromoProjection> promotions) {
        return new VenuePublicResponse()
                .id(venue.id())
                .name(venue.name())
                .address(venue.address())
                .lat(venue.lat())
                .lng(venue.lng())
                .description(venue.description())
                .category(VenuePublicResponse.CategoryEnum.fromValue(
                        venue.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(toPublicPhotoUri(venue.photoObjectKey()))
                .dishOfDay(venue.dishOfDay())
                .music(venue.music())
                .peopleNow(0)
                .createdAt(venue.createdAt())
                .updatedAt(venue.updatedAt())
                .promotions(toPromoResponses(promotions));
    }

    public VenueSearchResultResponse toSearchResponse(final VenueSearchProjection venue) {
        return new VenueSearchResultResponse()
                .id(venue.id())
                .name(venue.name())
                .address(venue.address())
                .lat(venue.lat())
                .lng(venue.lng())
                .category(VenueSearchResultResponse.CategoryEnum.fromValue(
                        venue.category().name().toLowerCase(Locale.ROOT)))
                .photoUrl(toPublicPhotoUri(venue.photoObjectKey()));
    }

    public URI toPublicPhotoUri(final String objectKey) {
        final String publicUrl = minioUrlBuilder.buildPublicUrl(objectKey);
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        return URI.create(publicUrl);
    }

    public List<VenuePromoResponse> toPromoResponses(final List<VenuePromoProjection> promotions) {
        return promotions.stream()
                .map(this::toPromoResponse)
                .toList();
    }

    private VenuePromoResponse toPromoResponse(final VenuePromoProjection promo) {
        return new VenuePromoResponse()
                .id(promo.id())
                .venueId(promo.venueId())
                .title(promo.title())
                .description(promo.description())
                .startsAt(promo.startsAt())
                .endsAt(promo.endsAt())
                .createdAt(promo.createdAt());
    }
}
