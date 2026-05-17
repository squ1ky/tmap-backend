package ru.tbank.tmap.user.domain;

import java.util.UUID;

public final class UserTestFactory {

    public static final UUID DEFAULT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String DEFAULT_EMAIL = "user@example.com";
    public static final String DEFAULT_PASSWORD_HASH = "hash";
    public static final String DEFAULT_NICKNAME = "Tatarin";

    private UserTestFactory() {
    }

    public static User createUser() {
        return createUser(UserRole.USER, false);
    }

    public static User createUser(final boolean blocked) {
        return createUser(UserRole.USER, blocked);
    }

    public static User createUser(final UserRole role, final boolean blocked) {
        final User user = new User(
                DEFAULT_ID,
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD_HASH,
                DEFAULT_NICKNAME,
                role
        );
        if (blocked) {
            user.block();
        }
        return user;
    }
}
