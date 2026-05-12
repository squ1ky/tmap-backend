package ru.tbank.tmap.auth.api;

import java.util.UUID;

public interface AuthAccountFacade {

    void changePassword(UUID userId, String currentPassword, String newPassword);
}
