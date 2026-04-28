package ru.tbank.tmap.loyalty.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;

public interface LoyaltyVerificationRepository extends JpaRepository<LoyaltyVerification, UUID> {

    long countByRuleId(UUID ruleId);

    @Query("""
            select new ru.tbank.tmap.loyalty.repository.LoyaltyVerificationRepository.LoyaltyRuleUsageCount(
                lv.rule.id,
                count(lv)
            )
            from LoyaltyVerification lv
            where lv.rule.id in :ruleIds
            group by lv.rule.id
            """)
    List<LoyaltyRuleUsageCount> countUsagesByRuleIds(@Param("ruleIds") Collection<UUID> ruleIds);

    record LoyaltyRuleUsageCount(UUID ruleId, long usages) {
    }
}
