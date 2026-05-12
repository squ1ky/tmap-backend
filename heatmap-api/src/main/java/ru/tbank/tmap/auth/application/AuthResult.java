package ru.tbank.tmap.auth.application;

import java.util.UUID;

public record AuthResult(
        UUID userId,
        String email,
        String nickname,
        String role,
        String accessToken,
        String plainRefreshToken
) {
}
