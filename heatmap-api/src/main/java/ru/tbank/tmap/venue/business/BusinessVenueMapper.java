package ru.tbank.tmap.venue.business;

import java.util.UUID;
import org.openapitools.model.VenueCreateRequest;
import org.openapitools.model.VenueUpdateRequest;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;

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
        venue.setDishOfDay(command.dishOfDay());
        venue.setMusic(command.music());
        return venue;
    }

    public VenuePendingUpdate toPendingUpdate(
            final Venue venue,
            final VenueUpdateCommand command,
            final long h3Res9
    ) {
        final VenuePendingUpdate pendingUpdate = new VenuePendingUpdate(venue);
        updatePendingUpdate(pendingUpdate, command, h3Res9);
        return pendingUpdate;
    }

    public void updatePendingUpdate(
            final VenuePendingUpdate pendingUpdate,
            final VenueUpdateCommand command,
            final long h3Res9
    ) {
        pendingUpdate.setName(command.name());
        pendingUpdate.setAddress(command.address());
        pendingUpdate.setLocation(command.location());
        pendingUpdate.setH3Res9(h3Res9);
        pendingUpdate.setCategory(command.category());
        pendingUpdate.setDescription(command.description());
        pendingUpdate.setDishOfDay(command.dishOfDay());
        pendingUpdate.setMusic(command.music());
        pendingUpdate.setStatus(VenueStatus.PENDING_UPDATE);
        pendingUpdate.setRejectReason(null);
    }

    public void applyPayloadToVenue(
            final Venue venue,
            final VenueUpdateCommand command,
            final long h3Res9
    ) {
        venue.setName(command.name());
        venue.setAddress(command.address());
        venue.setLocation(command.location());
        venue.setH3Res9(h3Res9);
        venue.setCategory(command.category());
        venue.setDescription(command.description());
        venue.setDishOfDay(command.dishOfDay());
        venue.setMusic(command.music());
    }

    public void applyPendingUpdateToVenue(final VenuePendingUpdate pendingUpdate, final Venue venue) {
        applyPayloadToVenue(
                venue,
                new VenueUpdateCommand(
                        pendingUpdate.getName(),
                        pendingUpdate.getAddress(),
                        pendingUpdate.getLocation(),
                        pendingUpdate.getCategory(),
                        pendingUpdate.getDescription(),
                        pendingUpdate.getDishOfDay(),
                        pendingUpdate.getMusic()
                ),
                pendingUpdate.getH3Res9()
        );
    }
}
