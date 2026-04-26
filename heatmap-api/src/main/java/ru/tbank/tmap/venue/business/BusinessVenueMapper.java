package ru.tbank.tmap.venue.business;

import java.util.UUID;
import org.openapitools.model.VenueCreateRequest;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;

@Component
public class BusinessVenueMapper {

    public Venue toEntity(
            final VenueCreateRequest request,
            final User owner,
            final GeoPoint location,
            final long h3Res9
    ) {
        final Venue venue = new Venue(
                UUID.randomUUID(),
                owner,
                request.getName(),
                request.getAddress(),
                location,
                h3Res9,
                VenueCategory.fromString(request.getCategory().getValue())
        );
        venue.setDescription(request.getDescription());
        venue.setPhotoUrl(request.getPhotoUrl() == null ? null : request.getPhotoUrl().toString());
        venue.setDishOfDay(request.getDishOfDay());
        venue.setMusic(request.getMusic());
        return venue;
    }
}
