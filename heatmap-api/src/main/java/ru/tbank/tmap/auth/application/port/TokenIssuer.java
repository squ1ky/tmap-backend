package ru.tbank.tmap.auth.application.port;

import java.util.UUID;

public interface TokenIssuer {

    String generateAccessToken(UUID userId, String email, String role);

    boolean isValidAccessToken(String token);

    String extractEmail(String token);

    UUID extractUserId(String token);

    String extractRole(String token);
}
