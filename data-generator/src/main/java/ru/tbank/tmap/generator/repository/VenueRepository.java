package ru.tbank.tmap.generator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.generator.domain.Venue;

import java.util.List;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    @Query("SELECT v FROM Venue v WHERE v.status = 'ACTIVE'")
    List<Venue> findAllActive();
}
