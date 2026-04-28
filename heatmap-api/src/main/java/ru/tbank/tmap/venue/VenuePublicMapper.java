package ru.tbank.tmap.venue;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.VenuePublicResponse;
import org.openapitools.model.VenueSearchResultResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.venue.repository.VenuePublicRow;
import ru.tbank.tmap.venue.search.VenueSearchResult;

@Component
@RequiredArgsConstructor
public class VenuePublicMapper {

    private final MinioUrlBuilder minioUrlBuilder;

    public VenuePublicResponse toResponse(final VenuePublicRow venue) {
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
                .promotions(List.of());
    }

    public VenueSearchResultResponse toSearchResponse(final VenueSearchResult venue) {
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
}
