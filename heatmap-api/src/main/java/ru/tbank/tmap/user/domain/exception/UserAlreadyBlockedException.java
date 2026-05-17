package ru.tbank.tmap.user.domain.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserAlreadyBlockedException extends RuntimeException {

    private final UUID id;

    public UserAlreadyBlockedException(final UUID id) {
        super("User already blocked: " + id);
        this.id = id;
    }
}
