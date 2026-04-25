package ru.tbank.tmap.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.user.User;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String USER_ID_CLAIM = "userId";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    /* default */ void init() {
        final byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(final User user) {
        final Date now = new Date();
        final Date expiration = new Date(now.getTime() + jwtProperties.accessExpiration().toMillis());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(USER_ID_CLAIM, user.getId().toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public boolean isValidAccessToken(final String token) {
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

    public String extractEmail(final String token) {
        return parse(token).getSubject();
    }

    public UUID extractUserId(final String token) {
        return UUID.fromString(parse(token).get(USER_ID_CLAIM, String.class));
    }

    public String extractRole(final String token) {
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
