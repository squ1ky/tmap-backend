package ru.tbank.tmap.venue.business;

import org.openapitools.model.VenueCreateRequest;
import org.openapitools.model.VenueUpdateRequest;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.venue.domain.VenueCategory;

@Component
public class BusinessVenueMapper {

    public VenueCreateCommand toCommand(final VenueCreateRequest request) {
        return new VenueCreateCommand(
                request.getName(),
                request.getAddress(),
                GeoPoint.of(request.getLat(), request.getLng()),
                VenueCategory.fromString(request.getCategory().getValue()),
                request.getDescription(),
                request.getDishOfDay(),
                request.getMusic()
        );
    }

    public VenueUpdateCommand toCommand(final VenueUpdateRequest request) {
        return new VenueUpdateCommand(
                request.getName(),
                request.getAddress(),
                GeoPoint.of(request.getLat(), request.getLng()),
                request.getCategory() == null ? null : VenueCategory.fromString(request.getCategory().getValue()),
                request.getDescription(),
                request.getDishOfDay(),
                request.getMusic()
        );
    }
}
