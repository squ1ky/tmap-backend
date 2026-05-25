package ru.tbank.tmap.loyalty.domain.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tbank.tmap.loyalty.application.query.BusinessLoyaltyHistoryProjection;

public interface LoyaltyBusinessRepository {

    Page<BusinessLoyaltyHistoryProjection> findRuleHistory(UUID ruleId, Pageable pageable);
}
