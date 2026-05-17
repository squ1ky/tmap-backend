package ru.tbank.tmap.user.domain.exception;

import java.util.UUID;

public class UserNotBlockedException extends RuntimeException {
    public UserNotBlockedException(final UUID id) {
        super("User is not blocked: " + id);
    }
}
