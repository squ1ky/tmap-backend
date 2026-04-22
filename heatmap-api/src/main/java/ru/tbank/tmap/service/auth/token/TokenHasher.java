package ru.tbank.tmap.service.auth.token;

public interface TokenHasher {
    String hash(String plainToken);
}
