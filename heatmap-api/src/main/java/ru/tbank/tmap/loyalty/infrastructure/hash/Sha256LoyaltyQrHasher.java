package ru.tbank.tmap.loyalty.infrastructure.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.loyalty.domain.LoyaltyQrHasher;

@Component
public class Sha256LoyaltyQrHasher implements LoyaltyQrHasher {

    private static final String ALGORITHM = "SHA-256";

    @Override
    public String hash(final String plainToken) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            final byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " not available", e);
        }
    }
}
