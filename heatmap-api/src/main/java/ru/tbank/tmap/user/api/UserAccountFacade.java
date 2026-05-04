package ru.tbank.tmap.user.api;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountFacade {

    boolean existsByEmail(String email);

    Optional<UserView> findByEmail(String email);

    Optional<UserView> findById(UUID id);

    UserView register(String email, String passwordHash, String nickname);
}
