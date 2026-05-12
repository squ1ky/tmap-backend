package ru.tbank.tmap.loyalty.infrastructure.db.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;

public interface JpaLoyaltyRuleRepository extends JpaRepository<LoyaltyRule, UUID>, LoyaltyRuleRepository {

    @Override
    List<LoyaltyRule> findByVenueIdOrderByCreatedAtDescIdDesc(UUID venueId);
}
