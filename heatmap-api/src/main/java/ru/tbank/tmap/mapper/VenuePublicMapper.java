package ru.tbank.tmap.mapper;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.openapitools.model.VenuePublicResponse;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.repository.model.VenuePublicRow;

@Component
public class VenuePublicMapper {

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
                .photoUrl(toUri(venue.photoUrl()))
                .dishOfDay(venue.dishOfDay())
                .music(venue.music())
                .peopleNow(0)
                .createdAt(venue.createdAt())
                .updatedAt(venue.updatedAt())
                .promotions(List.of());
    }

    private URI toUri(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URI.create(value);
    }
}
