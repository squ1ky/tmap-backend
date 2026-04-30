package ru.tbank.tmap.loyalty.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;

import java.util.List;
import java.util.UUID;

public interface JpaLoyaltyRuleRepository extends JpaRepository<LoyaltyRule, UUID>, LoyaltyRuleRepository {

    @Override
    List<LoyaltyRule> findByVenueIdOrderByCreatedAtDescIdDesc(UUID venueId);
}
