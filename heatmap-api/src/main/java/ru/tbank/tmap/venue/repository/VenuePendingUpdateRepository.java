package ru.tbank.tmap.venue.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;

public interface VenuePendingUpdateRepository extends JpaRepository<VenuePendingUpdate, UUID> {

    @EntityGraph(attributePaths = {"venue", "venue.owner"})
    Optional<VenuePendingUpdate> findByVenueId(UUID venueId);

    @EntityGraph(attributePaths = {"venue", "venue.owner"})
    List<VenuePendingUpdate> findByVenueIdIn(Collection<UUID> venueIds);

    @EntityGraph(attributePaths = {"venue", "venue.owner"})
    Page<VenuePendingUpdate> findByStatus(VenueStatus status, Pageable pageable);
}
