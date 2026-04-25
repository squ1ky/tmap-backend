package ru.tbank.tmap.auth.refresh;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class Sha256TokenHasher implements RefreshTokenHasher {

    private static final String ALGORITHM = "SHA-256";

    @Override
    public String hash(String plainToken) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            final byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " not available", e);
        }
    }
}
