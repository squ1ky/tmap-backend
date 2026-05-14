package ru.tbank.tmap.loyalty.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.auth.application.port.RefreshTokenHasher;
import ru.tbank.tmap.loyalty.application.command.IssueLoyaltyQrCommand;
import ru.tbank.tmap.loyalty.application.query.LoyaltyQrView;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSession;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSessionRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.user.api.UserAccountFacade;

@ExtendWith(MockitoExtension.class)
class LoyaltyQrServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private LoyaltyRuleRepository loyaltyRuleRepository;

    @Mock
    private LoyaltyQrSessionRepository loyaltyQrSessionRepository;

    @Mock
    private UserAccountFacade userAccountFacade;

    @Mock
    private RefreshTokenHasher tokenHasher;

    @Captor
    private ArgumentCaptor<LoyaltyQrSession> sessionCaptor;

    private LoyaltyQrService loyaltyQrService;

    @BeforeEach
    void setUp() {
        loyaltyQrService = new LoyaltyQrService(
                loyaltyRuleRepository,
                loyaltyQrSessionRepository,
                userAccountFacade,
                tokenHasher,
                Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void issueQr_whenRuleIsValid_thenCreatesShortLivedQrPayload() {
        given(userAccountFacade.existsById(USER_ID)).willReturn(true);
        given(loyaltyRuleRepository.findById(RULE_ID))
                .willReturn(Optional.of(new LoyaltyRule(RULE_ID, VENUE_ID, "Discount 15%", 15, 100)));
        given(tokenHasher.hash(any())).willReturn("hashed-token");

        final LoyaltyQrView response = loyaltyQrService.issueQr(new IssueLoyaltyQrCommand(USER_ID, VENUE_ID, RULE_ID));

        org.mockito.Mockito.verify(loyaltyQrSessionRepository).save(sessionCaptor.capture());
        final LoyaltyQrSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUserId()).isEqualTo(USER_ID);
        assertThat(savedSession.getVenueId()).isEqualTo(VENUE_ID);
        assertThat(savedSession.getRuleId()).isEqualTo(RULE_ID);
        assertThat(savedSession.getExpiresAt()).isEqualTo(OffsetDateTime.parse("2026-05-14T10:02:00Z"));

        assertThat(response.venueId()).isEqualTo(VENUE_ID);
        assertThat(response.ruleId()).isEqualTo(RULE_ID);
        assertThat(response.expiresAt()).isEqualTo(OffsetDateTime.parse("2026-05-14T10:02:00Z"));
        assertThat(response.qrPayload()).startsWith("lqr:1:");
    }

    @Test
    void resolveActiveSessionForUpdate_whenPayloadIsInvalid_thenThrowsValidationError() {
        assertThatThrownBy(() -> loyaltyQrService.resolveActiveSessionForUpdate("broken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Loyalty QR payload format is invalid");
    }
}
