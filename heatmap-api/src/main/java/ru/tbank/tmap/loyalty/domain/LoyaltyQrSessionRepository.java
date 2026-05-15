package ru.tbank.tmap.loyalty.domain;

import java.util.Optional;

public interface LoyaltyQrSessionRepository {

    LoyaltyQrSession save(LoyaltyQrSession session);

    Optional<LoyaltyQrSession> findByTokenHashForUpdate(String tokenHash);
}
