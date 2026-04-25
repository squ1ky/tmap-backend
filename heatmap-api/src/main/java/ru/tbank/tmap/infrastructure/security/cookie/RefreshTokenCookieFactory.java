package ru.tbank.tmap.infrastructure.security.cookie;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.auth.jwt.JwtProperties;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    private static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth/refresh";

    private final JwtProperties jwtProperties;
    private final CookieSecurityProperties cookieSecurityProperties;

    public ResponseCookie create(final String plainRefreshToken) {
        return baseBuilder(plainRefreshToken)
                .maxAge(jwtProperties.refreshExpiration())
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(final String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecurityProperties.secure())
                .path(COOKIE_PATH)
                .sameSite(cookieSecurityProperties.sameSite());
    }
}
