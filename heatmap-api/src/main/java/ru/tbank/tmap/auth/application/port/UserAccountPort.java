package ru.tbank.tmap.auth.application.port;

import ru.tbank.tmap.user.api.UserView;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountPort {

    Optional<UserView> findByEmail(String email);

    Optional<UserView> findById(UUID id);

    /**
     * @throws ru.tbank.tmap.user.api.exception.EmailAlreadyExistsException
     *         if a user with the given email already exists
     */
    UserView register(String email, String passwordHash, String nickname);

    UserView updatePasswordHash(UUID id, String passwordHash);
}
