package ru.tbank.tmap.venue.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository {

    Venue save(Venue venue);

    Optional<Venue> findById(UUID id);

    void deleteById(UUID id);

    Page<Venue> findByStatus(VenueStatus status, Pageable pageable);

    List<Venue> findByOwnerIdOrderByNameAscIdAsc(UUID ownerId);

    Optional<Venue> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
