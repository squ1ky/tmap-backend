package ru.tbank.tmap.loyalty.application.port;

public interface LoyaltyQrHasher {
    String hash(String plainToken);
}
