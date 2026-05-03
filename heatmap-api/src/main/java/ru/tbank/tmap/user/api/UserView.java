package ru.tbank.tmap.user.api;

import ru.tbank.tmap.user.domain.UserRole;

import java.util.UUID;

public record UserView(
        UUID id,
        String email,
        String passwordHash,
        String nickname,
        UserRole role,
        boolean blocked
) {}
