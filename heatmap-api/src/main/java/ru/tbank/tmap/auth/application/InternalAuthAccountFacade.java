package ru.tbank.tmap.auth.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.auth.api.AuthAccountFacade;
import ru.tbank.tmap.auth.application.service.AuthService;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalAuthAccountFacade implements AuthAccountFacade {

    private final AuthService authService;

    @Override
    public void changePassword(final UUID userId, final String currentPassword, final String newPassword) {
        authService.changePassword(userId, currentPassword, newPassword);
    }
}
