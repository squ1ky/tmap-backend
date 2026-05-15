package ru.tbank.tmap.profile.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.ChangePasswordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.auth.api.AuthAccountFacade;
import ru.tbank.tmap.loyalty.api.LoyaltyProfileFacade;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.api.UserView;
import ru.tbank.tmap.user.domain.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserAccountFacade userAccountFacade;
    private final AuthAccountFacade authAccountFacade;
    private final LoyaltyProfileFacade loyaltyProfileFacade;

    public UserView getProfile(final UUID userId) {
        return userAccountFacade.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
    }

    @Transactional
    public void changePassword(final UUID userId, final ChangePasswordRequest request) {
        authAccountFacade.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
    }

    public Page<LoyaltyHistoryProjection> getLoyaltyHistory(final UUID userId, final int page, final int size) {
        return loyaltyProfileFacade.findUserLoyaltyHistory(userId, PageRequest.of(page, size));
    }
}
