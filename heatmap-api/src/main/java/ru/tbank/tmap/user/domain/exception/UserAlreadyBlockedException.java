package ru.tbank.tmap.user.domain.exception;

import java.util.UUID;

public class UserAlreadyBlockedException extends RuntimeException {
    public UserAlreadyBlockedException(final UUID id) {
        super("User already blocked: " + id);
    }
}
