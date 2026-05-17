package ru.tbank.tmap.user.domain.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final UUID id;
    private final String email;

    private UserNotFoundException(final UUID id, final String email) {
        super("User not found");
        this.id = id;
        this.email = email;
    }

    public static UserNotFoundException byId(final UUID id) {
        return new UserNotFoundException(id, null);
    }

    public static UserNotFoundException byEmail(final String email) {
        return new UserNotFoundException(null, email);
    }
}
