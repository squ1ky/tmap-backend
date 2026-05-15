package ru.tbank.tmap.loyalty.domain;

public interface LoyaltyQrHasher {
    String hash(String plainToken);
}
