package ru.tbank.tmap.loyalty.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.api.LoyaltyBusinessFacade;
import ru.tbank.tmap.loyalty.application.query.BusinessLoyaltyHistoryProjection;
import ru.tbank.tmap.loyalty.domain.repository.LoyaltyBusinessRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalLoyaltyBusinessFacade implements LoyaltyBusinessFacade {

    private final LoyaltyBusinessRepository loyaltyBusinessRepository;

    @Override
    public Page<BusinessLoyaltyHistoryProjection> findRuleHistory(final UUID ruleId, final Pageable pageable) {
        return loyaltyBusinessRepository.findRuleHistory(ruleId, pageable);
    }
}
