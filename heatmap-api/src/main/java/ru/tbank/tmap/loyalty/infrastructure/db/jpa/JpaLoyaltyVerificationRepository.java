package ru.tbank.tmap.loyalty.infrastructure.db.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;

public interface JpaLoyaltyVerificationRepository
        extends JpaRepository<LoyaltyVerification, UUID>, LoyaltyVerificationRepository {

    @Override
    long countByRuleId(UUID ruleId);

    @Override
    boolean existsByRuleIdAndUserId(UUID ruleId, UUID userId);

    @Override
    void deleteByVenueId(UUID venueId);

    @Override
    @Query(value = """
        SELECT rule_id AS ruleId, COUNT(*) AS usageCount
        FROM loyalty_verifications
        WHERE rule_id IN :ruleIds
        GROUP BY rule_id
        """, nativeQuery = true)
    List<LoyaltyRuleUsageCount> countUsagesByRuleIds(@Param("ruleIds") Collection<UUID> ruleIds);

    @Override
    @Query(value = """
        SELECT rule_id
        FROM loyalty_verifications
        WHERE user_id = :userId
          AND rule_id IN :ruleIds
         """, nativeQuery = true)
    List<UUID> findUsedRuleIdsByUserIdAndRuleIds(@Param("userId") UUID userId,
                                                 @Param("ruleIds") Collection<UUID> ruleIds);
}
