package ru.tbank.tmap.venue.infrastructure.db.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

import java.util.UUID;

public interface JpaVenueRepository extends JpaRepository<Venue, UUID>, VenueRepository {
}
