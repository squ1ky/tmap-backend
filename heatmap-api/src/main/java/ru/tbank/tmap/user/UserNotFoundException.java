package ru.tbank.tmap.user;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final String email;

    public UserNotFoundException(final String email) {
        super("User not found");
        this.email = email;
    }
}
