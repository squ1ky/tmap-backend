package ru.tbank.tmap.auth.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.auth.application.port.UserAccountPort;
import ru.tbank.tmap.user.api.UserAccountFacade;
import ru.tbank.tmap.user.api.UserView;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserAccountAdapter implements UserAccountPort {

    private final UserAccountFacade userAccountFacade;

    @Override
    public Optional<UserView> findByEmail(String email) {
        return userAccountFacade.findByEmail(email);
    }

    @Override
    public Optional<UserView> findById(UUID id) {
        return userAccountFacade.findById(id);
    }

    @Override
    public UserView register(String email, String passwordHash, String nickname) {
        return userAccountFacade.register(email, passwordHash, nickname);
    }

    @Override
    public UserView updatePasswordHash(final UUID id, final String passwordHash) {
        return userAccountFacade.updatePasswordHash(id, passwordHash);
    }
}
