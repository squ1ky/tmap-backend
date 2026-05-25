package ru.tbank.tmap.loyalty.infrastructure.db.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.loyalty.application.query.BusinessLoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.domain.repository.LoyaltyBusinessRepository;

@JdbcTest
@Import({TestcontainersConfiguration.class, JdbcLoyaltyBusinessRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JdbcLoyaltyBusinessRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("91111111-1111-1111-1111-111111111111");
    private static final UUID RULE_ID = UUID.fromString("93333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_RULE_ID = UUID.fromString("93333333-3333-3333-3333-333333333334");
    private static final UUID VENUE_ID = UUID.fromString("92222222-2222-2222-2222-222222222222");
    private static final UUID GUEST_1_ID = UUID.fromString("95555555-5555-5555-5555-555555555555");
    private static final UUID GUEST_2_ID = UUID.fromString("96666666-6666-6666-6666-666666666666");
    private static final UUID GUEST_3_ID = UUID.fromString("97777777-7777-7777-7777-777777777777");

    @Autowired
    private LoyaltyBusinessRepository loyaltyBusinessRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findRuleHistory_whenRuleHasVerifications_thenReturnsSortedPaginatedPage() {
        insertUser(OWNER_ID, "owner@example.com", "BUSINESS_OWNER");
        insertUser(GUEST_1_ID, "guest1@example.com", "USER");
        insertUser(GUEST_2_ID, "guest2@example.com", "USER");
        insertUser(GUEST_3_ID, "guest3@example.com", "USER");
        insertVenue(VENUE_ID, OWNER_ID);
        insertRule(RULE_ID, VENUE_ID, "Discount 15%", 15, 100);
        insertRule(OTHER_RULE_ID, VENUE_ID, "Discount 30%", 30, 100);

        insertVerification(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                RULE_ID,
                GUEST_1_ID,
                15,
                OffsetDateTime.parse("2026-05-24T10:00:00+03:00")
        );
        insertVerification(
                UUID.fromString("44444444-4444-4444-4444-444444444445"),
                RULE_ID,
                GUEST_2_ID,
                20,
                OffsetDateTime.parse("2026-05-24T11:00:00+03:00")
        );
        insertVerification(
                UUID.fromString("44444444-4444-4444-4444-444444444446"),
                OTHER_RULE_ID,
                GUEST_3_ID,
                30,
                OffsetDateTime.parse("2026-05-24T12:00:00+03:00")
        );

        final Page<BusinessLoyaltyHistoryProjection> result =
                loyaltyBusinessRepository.findRuleHistory(RULE_ID, PageRequest.of(0, 1));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);

        final BusinessLoyaltyHistoryProjection item = result.getContent().getFirst();
        assertThat(item.id()).isEqualTo(UUID.fromString("44444444-4444-4444-4444-444444444445"));
        assertThat(item.venueId()).isEqualTo(VENUE_ID);
        assertThat(item.userId()).isEqualTo(GUEST_2_ID);
        assertThat(item.ruleId()).isEqualTo(RULE_ID);
        assertThat(item.discountApplied()).isEqualTo(20);
        assertThat(item.verifiedAt()).isEqualTo(OffsetDateTime.parse("2026-05-24T11:00:00+03:00"));
    }

    @Test
    void findRuleHistory_whenRuleHasNoVerifications_thenReturnsEmptyPage() {
        insertUser(OWNER_ID, "owner-empty@example.com", "BUSINESS_OWNER");
        insertVenue(VENUE_ID, OWNER_ID);
        insertRule(RULE_ID, VENUE_ID, "Discount 15%", 15, 100);

        final Page<BusinessLoyaltyHistoryProjection> result =
                loyaltyBusinessRepository.findRuleHistory(RULE_ID, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    private void insertUser(final UUID id, final String email, final String role) {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, nickname, role, blocked)
                VALUES (?, ?, 'password-hash', ?, ?, false)
                """,
                id,
                email,
                "user-" + id.toString().substring(0, 8),
                role
        );
    }

    private void insertVenue(final UUID venueId, final UUID ownerId) {
        jdbcTemplate.update(
                """
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, description, status, updated_at
                )
                VALUES (?, ?, 'History Venue', 'Kazan Test Address', 55.7900, 49.1200, ?, 'FOOD', 'History test venue', 'ACTIVE', now())
                """,
                venueId,
                ownerId,
                617733123456780000L + Math.abs(venueId.hashCode())
        );
    }

    private void insertRule(
            final UUID ruleId,
            final UUID venueId,
            final String description,
            final int discountPercent,
            final int maxUsages
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO loyalty_rules (id, venue_id, description, discount_percent, max_usages, active)
                VALUES (?, ?, ?, ?, ?, true)
                """,
                ruleId,
                venueId,
                description,
                discountPercent,
                maxUsages
        );
    }

    private void insertVerification(
            final UUID id,
            final UUID ruleId,
            final UUID userId,
            final int discountApplied,
            final OffsetDateTime verifiedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO loyalty_verifications (
                    id, venue_id, user_id, rule_id, discount_applied, verified_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id,
                VENUE_ID,
                userId,
                ruleId,
                discountApplied,
                verifiedAt
        );
    }
}
