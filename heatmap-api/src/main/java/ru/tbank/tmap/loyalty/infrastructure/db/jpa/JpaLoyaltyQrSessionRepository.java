package ru.tbank.tmap.loyalty.infrastructure.db.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSession;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrSessionRepository;

public interface JpaLoyaltyQrSessionRepository
        extends JpaRepository<LoyaltyQrSession, UUID>, LoyaltyQrSessionRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LoyaltyQrSession s where s.tokenHash = :tokenHash")
    Optional<LoyaltyQrSession> findByTokenHashForUpdate(String tokenHash);
}
