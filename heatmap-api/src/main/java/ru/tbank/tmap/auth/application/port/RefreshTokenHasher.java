package ru.tbank.tmap.auth.application.port;

public interface RefreshTokenHasher {
    String hash(String plainToken);
}
