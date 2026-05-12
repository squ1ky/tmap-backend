package ru.tbank.tmap.loyalty.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import ru.tbank.tmap.loyalty.application.query.LoyaltyRuleUsageCount;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerification;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;
import ru.tbank.tmap.profile.application.query.ProfileLoyaltyHistoryProjection;
import ru.tbank.tmap.profile.application.query.ProfileUsedPromoProjection;

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

    @Query(value = """
        SELECT
            lv.id AS id,
            lv.venue_id AS venueId,
            v.name AS venueName,
            lv.rule_id AS ruleId,
            lr.description AS ruleDescription,
            lv.discount_applied AS discountApplied,
            lv.verified_at AS verifiedAt
        FROM loyalty_verifications lv
        JOIN venues v ON v.id = lv.venue_id
        JOIN loyalty_rules lr ON lr.id = lv.rule_id
        WHERE lv.user_id = :userId
        ORDER BY lv.verified_at DESC, lv.id DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM loyalty_verifications lv
        WHERE lv.user_id = :userId
        """,
            nativeQuery = true)
    Page<ProfileLoyaltyHistoryProjection> findProfileHistoryByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query(value = """
        SELECT
            v.name AS venueName,
            lr.description AS description,
            lv.discount_applied AS discountPercent,
            lv.verified_at AS usedAt
        FROM loyalty_verifications lv
        JOIN venues v ON v.id = lv.venue_id
        JOIN loyalty_rules lr ON lr.id = lv.rule_id
        WHERE lv.user_id = :userId
        ORDER BY lv.verified_at DESC, lv.id DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM loyalty_verifications lv
        WHERE lv.user_id = :userId
        """,
            nativeQuery = true)
    Page<ProfileUsedPromoProjection> findUsedPromosByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
