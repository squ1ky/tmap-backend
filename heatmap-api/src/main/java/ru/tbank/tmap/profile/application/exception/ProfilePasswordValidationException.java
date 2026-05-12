package ru.tbank.tmap.profile.application.exception;

public class ProfilePasswordValidationException extends RuntimeException {

    public ProfilePasswordValidationException() {
        super("New password must be different from current password");
    }
}
