package ru.tbank.tmap.loyalty.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;

public interface LoyaltyRuleRepository extends JpaRepository<LoyaltyRule, UUID> {

    @EntityGraph(attributePaths = {"venue", "venue.owner"})
    List<LoyaltyRule> findByVenueIdAndVenueOwnerIdOrderByCreatedAtDescIdDesc(UUID venueId, UUID ownerId);

    @EntityGraph(attributePaths = {"venue", "venue.owner"})
    Optional<LoyaltyRule> findByIdAndVenueOwnerId(UUID id, UUID ownerId);
}
