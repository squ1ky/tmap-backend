package ru.tbank.tmap.auth.application.exception;

public class PasswordChangeValidationException extends RuntimeException {

    public PasswordChangeValidationException() {
        super("New password must be different from current password");
    }
}
