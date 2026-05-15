package ru.tbank.tmap.loyalty.infrastructure.db.jdbc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.domain.repository.LoyaltyProfileRepository;

@Repository
public class JdbcLoyaltyProfileRepository implements LoyaltyProfileRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataClassRowMapper<LoyaltyHistoryProjection> loyaltyHistoryRowMapper =
            new DataClassRowMapper<>(LoyaltyHistoryProjection.class);

    public JdbcLoyaltyProfileRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<LoyaltyHistoryProjection> findUserLoyaltyHistory(
            final UUID userId,
            final Pageable pageable
    ) {
        final String sql = """
                SELECT
                    lv.id,
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
                LIMIT :limit OFFSET :offset
                """;
        final Map<String, Object> params = Map.of(
                "userId", userId,
                "limit", pageable.getPageSize(),
                "offset", pageable.getOffset()
        );
        final List<LoyaltyHistoryProjection> items = jdbcTemplate.query(sql, params, loyaltyHistoryRowMapper);

        return new PageImpl<>(items, pageable, countByUserId(userId));
    }

    private long countByUserId(final UUID userId) {
        final String sql = """
                SELECT COUNT(*)
                FROM loyalty_verifications
                WHERE user_id = :userId
                """;
        return jdbcTemplate.queryForObject(sql, Map.of("userId", userId), Long.class);
    }
}
