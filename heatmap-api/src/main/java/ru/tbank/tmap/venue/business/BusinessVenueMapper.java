package ru.tbank.tmap.venue.business;

import java.util.UUID;
import org.openapitools.model.VenueCreateRequest;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.User;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueCreateCommand;

@Component
public class BusinessVenueMapper {

    public VenueCreateCommand toCommand(final VenueCreateRequest request) {
        return new VenueCreateCommand(
                request.getName(),
                request.getAddress(),
                GeoPoint.of(request.getLat(), request.getLng()),
                VenueCategory.fromString(request.getCategory().getValue()),
                request.getDescription(),
                request.getPhotoUrl() == null ? null : request.getPhotoUrl().toString(),
                request.getDishOfDay(),
                request.getMusic()
        );
    }

    public Venue toEntity(
            final VenueCreateCommand command,
            final User owner,
            final long h3Res9
    ) {
        final Venue venue = new Venue(
                UUID.randomUUID(),
                owner,
                command.name(),
                command.address(),
                command.location(),
                h3Res9,
                command.category()
        );
        venue.setDescription(command.description());
        venue.setPhotoUrl(command.photoUrl());
        venue.setDishOfDay(command.dishOfDay());
        venue.setMusic(command.music());
        return venue;
    }
}
