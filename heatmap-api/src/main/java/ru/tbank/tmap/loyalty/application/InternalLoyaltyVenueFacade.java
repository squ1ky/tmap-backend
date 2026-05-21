package ru.tbank.tmap.loyalty.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.loyalty.api.LoyaltyVenueFacade;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalLoyaltyVenueFacade implements LoyaltyVenueFacade {

    private final LoyaltyVerificationRepository loyaltyVerificationRepository;

    @Override
    public void deleteVerificationHistory(final UUID venueId) {
        loyaltyVerificationRepository.deleteByVenueId(venueId);
    }
}
