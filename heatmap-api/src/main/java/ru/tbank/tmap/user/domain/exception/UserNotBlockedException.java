package ru.tbank.tmap.user.domain.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserNotBlockedException extends RuntimeException {

    private final UUID id;

    public UserNotBlockedException(final UUID id) {
        super("User is not blocked: " + id);
        this.id = id;
    }
}
