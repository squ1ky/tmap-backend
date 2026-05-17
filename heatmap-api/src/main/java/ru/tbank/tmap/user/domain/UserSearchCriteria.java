package ru.tbank.tmap.user.domain;

import java.time.OffsetDateTime;

public record UserSearchCriteria(
        String nickname,
        String email,
        UserRole role,
        Boolean blocked,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo
) {
}
