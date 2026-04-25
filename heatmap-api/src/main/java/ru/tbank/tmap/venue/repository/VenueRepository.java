package ru.tbank.tmap.venue.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueStatus;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    Page<Venue> findByStatus(VenueStatus status, Pageable pageable);
}
