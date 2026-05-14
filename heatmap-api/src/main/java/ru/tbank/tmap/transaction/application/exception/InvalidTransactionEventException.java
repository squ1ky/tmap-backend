package ru.tbank.tmap.transaction.application.exception;

public class InvalidTransactionEventException extends RuntimeException {

    private final int batchIndex;

    public InvalidTransactionEventException(int batchIndex, String message, Throwable cause) {
        super(message, cause);
        this.batchIndex = batchIndex;
    }

    public int batchIndex() {
        return batchIndex;
    }
}