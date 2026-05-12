package ru.tbank.tmap.profile.domain.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tbank.tmap.profile.application.query.ProfileLoyaltyHistoryProjection;
import ru.tbank.tmap.profile.application.query.ProfileUsedPromoProjection;

public interface ProfileRepository {

    Page<ProfileLoyaltyHistoryProjection> findLoyaltyHistoryByUserId(UUID userId, Pageable pageable);

    Page<ProfileUsedPromoProjection> findUsedPromosByUserId(UUID userId, Pageable pageable);
}
