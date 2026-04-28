package ru.tbank.tmap.infrastructure.minio;

public class ObjectStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ObjectStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
