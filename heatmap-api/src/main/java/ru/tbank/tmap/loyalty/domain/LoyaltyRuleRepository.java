package ru.tbank.tmap.loyalty.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyRuleRepository {

    LoyaltyRule save(LoyaltyRule rule);

    Optional<LoyaltyRule> findById(UUID id);

    Optional<LoyaltyRule> findByIdForUpdate(UUID id);

    List<LoyaltyRule> findByVenueIdOrderByCreatedAtDescIdDesc(UUID venueId);

    List<LoyaltyRule> findByVenueIdAndActiveTrueOrderByCreatedAtDescIdDesc(UUID venueId);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
