package ru.tbank.tmap.venue.presentation.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.tmap.TestcontainersConfiguration;
import ru.tbank.tmap.auth.infrastructure.security.CustomUserDetails;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class BusinessVenueDeletionIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID VERIFICATION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TRANSACTION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    loyalty_qr_sessions,
                    loyalty_verifications,
                    loyalty_rules,
                    venue_pending_updates,
                    transactions,
                    venues,
                    refresh_tokens,
                    users
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void deleteVenue_whenVenueHasNoBlockingHistory_thenDeletesIt() throws Exception {
        insertUser();
        insertVenue();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ownerPrincipal(),
                null,
                ownerPrincipal().getAuthorities()));
        try {
            mockMvc.perform(delete("/api/v1/business/venues/{id}", VENUE_ID)
                            .with(user(ownerPrincipal()))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(countRows("venues", VENUE_ID)).isZero();
    }

    @Test
    void deleteVenue_whenVenueHasLoyaltyHistory_thenDeletesVenueAndHistory() throws Exception {
        insertUser();
        insertVenue();
        insertLoyaltyRule();
        insertLoyaltyVerification();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ownerPrincipal(),
                null,
                ownerPrincipal().getAuthorities()));
        try {
            mockMvc.perform(delete("/api/v1/business/venues/{id}", VENUE_ID)
                            .with(user(ownerPrincipal()))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(countRows("venues", VENUE_ID)).isZero();
        assertThat(countByColumn("loyalty_verifications", "venue_id", VENUE_ID)).isZero();
    }

    @Test
    void deleteVenue_whenVenueHasTransactions_thenDeletesVenueAndTransactions() throws Exception {
        insertUser();
        insertVenue();
        insertTransaction();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ownerPrincipal(),
                null,
                ownerPrincipal().getAuthorities()));
        try {
            mockMvc.perform(delete("/api/v1/business/venues/{id}", VENUE_ID)
                            .with(user(ownerPrincipal()))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(countRows("venues", VENUE_ID)).isZero();
        assertThat(countByColumn("transactions", "venue_id", VENUE_ID)).isZero();
    }

    private void insertUser() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, role, blocked)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                OWNER_ID,
                "owner@example.com",
                "password-hash",
                "owner",
                "BUSINESS_OWNER",
                false
        );
    }

    private void insertVenue() {
        jdbcTemplate.update("""
                INSERT INTO venues (
                    id, owner_id, name, address, lat, lng, h3_res9, category, description, status, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                VENUE_ID,
                OWNER_ID,
                "Bar One",
                "Kazan Center, 2",
                55.7905,
                49.1140,
                617422037122678783L,
                "ENTERTAINMENT",
                "Default description",
                "ACTIVE",
                OffsetDateTime.now()
        );
    }

    private void insertLoyaltyRule() {
        jdbcTemplate.update("""
                INSERT INTO loyalty_rules (
                    id, venue_id, description, discount_percent, max_usages, active
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                RULE_ID,
                VENUE_ID,
                "Discount 15%",
                BigDecimal.valueOf(15),
                100,
                true
        );
    }

    private void insertLoyaltyVerification() {
        jdbcTemplate.update("""
                INSERT INTO loyalty_verifications (
                    id, venue_id, user_id, rule_id, discount_applied
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                VERIFICATION_ID,
                VENUE_ID,
                OWNER_ID,
                RULE_ID,
                BigDecimal.valueOf(15)
        );
    }

    private void insertTransaction() {
        jdbcTemplate.update("""
                INSERT INTO transactions (
                    id, venue_id, amount, lat, lng, h3_res7, h3_res8, h3_res9, category, occurred_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                TRANSACTION_ID,
                VENUE_ID,
                BigDecimal.valueOf(1500),
                55.7905,
                49.1140,
                607533113720381439L,
                612036938201382911L,
                617422037122678783L,
                "ENTERTAINMENT",
                OffsetDateTime.now().minusDays(1)
        );
    }

    private long countRows(final String tableName, final UUID id) {
        return countByColumn(tableName, "id", id);
    }

    private long countByColumn(final String tableName, final String columnName, final UUID value) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                value
        );
    }

    private CustomUserDetails ownerPrincipal() {
        return new CustomUserDetails(
                OWNER_ID,
                "owner@example.com",
                "password-hash",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_BUSINESS_OWNER"))
        );
    }
}
