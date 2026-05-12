package ru.tbank.tmap.loyalty.domain.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.application.query.UsedPromoProjection;

public interface LoyaltyProfileRepository {

    Page<LoyaltyHistoryProjection> findUserLoyaltyHistory(UUID userId, Pageable pageable);

    Page<UsedPromoProjection> findUserUsedPromos(UUID userId, Pageable pageable);
}
