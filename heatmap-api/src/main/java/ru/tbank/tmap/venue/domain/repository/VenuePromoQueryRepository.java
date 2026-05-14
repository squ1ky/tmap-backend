package ru.tbank.tmap.venue.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.tbank.tmap.venue.application.query.VenuePromoProjection;

public interface VenuePromoQueryRepository {

    List<VenuePromoProjection> findActiveByVenueId(UUID venueId);

    Map<UUID, List<VenuePromoProjection>> findActiveByVenueIds(Collection<UUID> venueIds);
}
