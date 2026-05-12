package ru.tbank.tmap.loyalty.api;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.application.query.UsedPromoProjection;

public interface LoyaltyProfileFacade {

    Page<LoyaltyHistoryProjection> findUserLoyaltyHistory(UUID userId, Pageable pageable);

    Page<UsedPromoProjection> findUserUsedPromos(UUID userId, Pageable pageable);
}
