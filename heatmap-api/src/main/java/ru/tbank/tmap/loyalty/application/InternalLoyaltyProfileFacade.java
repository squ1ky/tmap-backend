package ru.tbank.tmap.loyalty.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.api.LoyaltyProfileFacade;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.application.query.UsedPromoProjection;
import ru.tbank.tmap.loyalty.domain.repository.LoyaltyProfileRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalLoyaltyProfileFacade implements LoyaltyProfileFacade {

    private final LoyaltyProfileRepository loyaltyProfileRepository;

    @Override
    public Page<LoyaltyHistoryProjection> findUserLoyaltyHistory(final UUID userId, final Pageable pageable) {
        return loyaltyProfileRepository.findUserLoyaltyHistory(userId, pageable);
    }

    @Override
    public Page<UsedPromoProjection> findUserUsedPromos(final UUID userId, final Pageable pageable) {
        return loyaltyProfileRepository.findUserUsedPromos(userId, pageable);
    }
}
