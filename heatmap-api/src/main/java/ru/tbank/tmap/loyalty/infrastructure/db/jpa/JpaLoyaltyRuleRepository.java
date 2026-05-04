package ru.tbank.tmap.loyalty.infrastructure.db.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;

public interface JpaLoyaltyRuleRepository extends JpaRepository<LoyaltyRule, UUID>, LoyaltyRuleRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lr from LoyaltyRule lr where lr.id = :id")
    Optional<LoyaltyRule> findByIdForUpdate(UUID id);

    @Override
    List<LoyaltyRule> findByVenueIdOrderByCreatedAtDescIdDesc(UUID venueId);
}
