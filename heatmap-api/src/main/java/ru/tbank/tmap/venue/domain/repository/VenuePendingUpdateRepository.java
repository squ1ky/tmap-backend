package ru.tbank.tmap.venue.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenuePendingUpdateRepository {

    VenuePendingUpdate save(VenuePendingUpdate pendingUpdate);

    Optional<VenuePendingUpdate> findByVenueId(UUID venueId);

    List<VenuePendingUpdate> findByVenueIdIn(Collection<UUID> venueIds);

    Page<VenuePendingUpdate> findByStatus(VenueStatus status, Pageable pageable);

    void deleteById(UUID venueId);
}
