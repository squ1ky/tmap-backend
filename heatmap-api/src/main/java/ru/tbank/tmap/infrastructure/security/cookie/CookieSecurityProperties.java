package ru.tbank.tmap.infrastructure.security.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.cookie")
public record CookieSecurityProperties(
        boolean secure,
        String sameSite
) {
}
