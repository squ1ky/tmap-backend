package ru.tbank.tmap.dto.auth;

import ru.tbank.tmap.domain.user.UserRole;

import java.util.UUID;

public record AuthResult(
        UUID userId,
        UserRole role,
        String accessToken,
        String plainRefreshToken
) {
}
