package ru.tbank.tmap.auth;

import ru.tbank.tmap.user.UserRole;

import java.util.UUID;

public record AuthResult(
        UUID userId,
        UserRole role,
        String accessToken,
        String plainRefreshToken
) {
}
