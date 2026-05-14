package ru.tbank.tmap.loyalty.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.auth.application.port.RefreshTokenHasher;
import ru.tbank.tmap.loyalty.application.command.IssueLoyaltyQrCommand;
import ru.tbank.tmap.loyalty.application.query.LoyaltyQrView;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSession;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSessionRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyQrValidationException;
import ru.tbank.tmap.loyalty.domain.exception.LoyaltyRuleNotFoundException;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyQrService {

    private static final int QR_TTL_SECONDS = 120;
    private static final int QR_TOKEN_BYTE_LENGTH = 32;
    private static final String QR_PAYLOAD_PREFIX = "lqr:1:";

    private final LoyaltyRuleRepository loyaltyRuleRepository;
    private final LoyaltyQrSessionRepository loyaltyQrSessionRepository;
    private final UserAccountFacade userAccountFacade;
    private final RefreshTokenHasher tokenHasher;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public LoyaltyQrView issueQr(final IssueLoyaltyQrCommand command) {
        if (!userAccountFacade.existsById(command.userId())) {
            throw new UserNotFoundException(command.userId().toString());
        }

        final LoyaltyRule rule = loyaltyRuleRepository.findById(command.ruleId())
                .orElseThrow(() -> new LoyaltyRuleNotFoundException(command.ruleId()));

        if (!rule.getVenueId().equals(command.venueId())) {
            throw LoyaltyQrValidationException.ruleDoesNotBelongToRequestedVenue();
        }
        if (!rule.isActive()) {
            throw LoyaltyQrValidationException.inactiveRuleCannotGenerateQr();
        }

        final String plainToken = generatePlainToken();
        final OffsetDateTime expiresAt = OffsetDateTime.now(clock).plusSeconds(QR_TTL_SECONDS);

        loyaltyQrSessionRepository.save(new LoyaltyQrSession(
                UUID.randomUUID(),
                tokenHasher.hash(plainToken),
                command.userId(),
                command.venueId(),
                command.ruleId(),
                expiresAt
        ));

        return new LoyaltyQrView(
                command.venueId(),
                command.ruleId(),
                QR_PAYLOAD_PREFIX + plainToken,
                expiresAt
        );
    }

    public LoyaltyQrSession resolveActiveSessionForUpdate(final String qrPayload) {
        final String plainToken = extractPlainToken(qrPayload);
        final String tokenHash = tokenHasher.hash(plainToken);
        final LoyaltyQrSession session = loyaltyQrSessionRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(LoyaltyQrValidationException::invalidQr);

        final OffsetDateTime now = OffsetDateTime.now(clock);
        if (session.isConsumed()) {
            throw LoyaltyQrValidationException.usedQr();
        }
        if (!session.getExpiresAt().isAfter(now)) {
            throw LoyaltyQrValidationException.expiredQr();
        }

        return session;
    }

    @Transactional
    public void markConsumed(final LoyaltyQrSession session) {
        session.setConsumedAt(OffsetDateTime.now(clock));
        loyaltyQrSessionRepository.save(session);
    }

    private String generatePlainToken() {
        final byte[] bytes = new byte[QR_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extractPlainToken(final String qrPayload) {
        if (qrPayload == null || qrPayload.isBlank()) {
            throw LoyaltyQrValidationException.missingQrPayload();
        }
        if (!qrPayload.startsWith(QR_PAYLOAD_PREFIX)) {
            throw LoyaltyQrValidationException.invalidQrPayloadFormat();
        }
        final String plainToken = qrPayload.substring(QR_PAYLOAD_PREFIX.length());
        if (plainToken.isBlank()) {
            throw LoyaltyQrValidationException.invalidQrPayloadFormat();
        }
        return plainToken;
    }
}
