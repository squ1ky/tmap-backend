package ru.tbank.tmap.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.auth.application.port.TokenIssuer;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JjwtTokenIssuer implements TokenIssuer {

    private static final String ROLE_CLAIM = "role";
    private static final String USER_ID_CLAIM = "userId";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    /* default */ void init() {
        final byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateAccessToken(UUID userId, String email, String role) {
        final Date now = new Date();
        final Date expiration = new Date(now.getTime() + jwtProperties.accessExpiration().toMillis());

        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role)
                .claim(USER_ID_CLAIM, userId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public boolean isValidAccessToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).get(USER_ID_CLAIM, String.class));
    }

    @Override
    public String extractRole(String token) {
        return parse(token).get(ROLE_CLAIM, String.class);
    }

    private Claims parse(final String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
