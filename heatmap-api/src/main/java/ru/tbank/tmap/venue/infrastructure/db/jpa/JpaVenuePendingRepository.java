package ru.tbank.tmap.venue.infrastructure.db.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.repository.VenuePendingUpdateRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaVenuePendingRepository
        extends JpaRepository<VenuePendingUpdate, UUID>, VenuePendingUpdateRepository {

    @Override
    @EntityGraph(attributePaths = { "venue" })
    Optional<VenuePendingUpdate> findByVenueId(UUID venueId);

    @Override
    @EntityGraph(attributePaths = { "venue" })
    List<VenuePendingUpdate> findByVenueIdIn(Collection<UUID> venueIds);

    @Override
    @EntityGraph(attributePaths = { "venue" })
    Page<VenuePendingUpdate> findByStatus(VenueStatus status, Pageable pageable);
}
