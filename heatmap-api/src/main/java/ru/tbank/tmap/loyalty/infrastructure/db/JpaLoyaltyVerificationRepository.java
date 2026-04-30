package ru.tbank.tmap.loyalty.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;
import ru.tbank.tmap.loyalty.repository.LoyaltyRuleUsageCount;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaLoyaltyVerificationRepository
        extends JpaRepository<LoyaltyVerification, UUID>, LoyaltyVerificationRepository {

    @Override
    long countByRuleId(UUID ruleId);

    @Override
    @Query(value = """
        SELECT rule_id AS ruleId, COUNT(*) AS usageCount
        FROM loyalty_verifications
        WHERE rule_id IN :ruleIds
        GROUP BY rule_id
        """, nativeQuery = true)
    List<LoyaltyRuleUsageCount> countUsagesByRuleIds(@Param("ruleIds") Collection<UUID> ruleIds);
}
