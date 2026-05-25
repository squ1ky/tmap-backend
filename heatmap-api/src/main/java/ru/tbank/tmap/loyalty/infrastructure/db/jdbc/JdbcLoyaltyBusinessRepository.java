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
import ru.tbank.tmap.loyalty.application.query.BusinessLoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.domain.repository.LoyaltyBusinessRepository;

@Repository
public class JdbcLoyaltyBusinessRepository implements LoyaltyBusinessRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataClassRowMapper<BusinessLoyaltyHistoryProjection> rowMapper =
            new DataClassRowMapper<>(BusinessLoyaltyHistoryProjection.class);

    public JdbcLoyaltyBusinessRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<BusinessLoyaltyHistoryProjection> findRuleHistory(final UUID ruleId, final Pageable pageable) {
        final String sql = """
                SELECT
                    lv.id,
                    lv.venue_id AS venueId,
                    lv.user_id AS userId,
                    lv.rule_id AS ruleId,
                    lv.discount_applied AS discountApplied,
                    lv.verified_at AS verifiedAt
                FROM loyalty_verifications lv
                WHERE lv.rule_id = :ruleId
                ORDER BY lv.verified_at DESC, lv.id DESC
                LIMIT :limit OFFSET :offset
                """;
        final Map<String, Object> params = Map.of(
                "ruleId", ruleId,
                "limit", pageable.getPageSize(),
                "offset", pageable.getOffset()
        );
        final List<BusinessLoyaltyHistoryProjection> items = jdbcTemplate.query(sql, params, rowMapper);

        return new PageImpl<>(items, pageable, countByRuleId(ruleId));
    }

    private long countByRuleId(final UUID ruleId) {
        final String sql = """
                SELECT COUNT(*)
                FROM loyalty_verifications
                WHERE rule_id = :ruleId
                """;
        return jdbcTemplate.queryForObject(sql, Map.of("ruleId", ruleId), Long.class);
    }
}
