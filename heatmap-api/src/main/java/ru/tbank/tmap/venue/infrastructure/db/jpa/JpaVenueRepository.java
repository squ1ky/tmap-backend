package ru.tbank.tmap.venue.infrastructure.db.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

import java.util.List;
import java.util.UUID;

public interface JpaVenueRepository extends JpaRepository<Venue, UUID>, VenueRepository {

    @Override
    @Query("SELECT v FROM Venue v WHERE v.ownerId = :ownerId ORDER BY v.content.name ASC, v.id ASC")
    List<Venue> findByOwnerIdOrderByNameAscIdAsc(@Param("ownerId") UUID ownerId);
}
