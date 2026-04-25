package ru.tbank.tmap.auth.refresh;

public interface RefreshTokenHasher {
    String hash(String plainToken);
}
