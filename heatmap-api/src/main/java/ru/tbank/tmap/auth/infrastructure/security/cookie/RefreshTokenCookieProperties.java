package ru.tbank.tmap.auth.infrastructure.security.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.cookie")
public record RefreshTokenCookieProperties(
        boolean secure,
        String sameSite
) {
}
