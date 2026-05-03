package ru.tbank.tmap.auth.refresh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.tbank.tmap.auth.infrastructure.hash.Sha256TokenHasher;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class Sha256TokenHasherTest {

    private Sha256TokenHasher tokenHasher;

    @BeforeEach
    void setUp() {
        tokenHasher = new Sha256TokenHasher();
    }

    @Test
    @DisplayName("Должен корректно хэшировать строку в Base64 (SHA-256)")
    void hash_whenPlainTokenProvided_thenReturnBase64EncodedSha256Hash() {
        String plainToken = "test";
        String expectedHash = "n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg=";

        String actualHash = tokenHasher.hash(plainToken);

        assertThat(actualHash).isNotBlank();
        assertThat(actualHash).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("Одинаковые строки должны давать одинаковый хэш")
    void hash_whenSameTokensProvided_thenReturnSameHashes() {
        String token = "my-secure-refresh-token-123";

        String hash1 = tokenHasher.hash(token);
        String hash2 = tokenHasher.hash(token);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Разные строки должны давать разный хэш")
    void hash_whenDifferentTokensProvided_thenReturnDifferentHashes() {
        String token1 = "refresh-token-A";
        String token2 = "refresh-token-B";

        String hash1 = tokenHasher.hash(token1);
        String hash2 = tokenHasher.hash(token2);

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
