package org.fincore.identity.user.infrastructure.security;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordHasherTest {

    private BCryptPasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new BCryptPasswordHasher();
    }

    @Test
    void shouldHashPassword() {

        // Given
        String rawPassword = "Password123!";

        // When
        String hash = hasher.hash(rawPassword);

        // Then
        assertNotNull(hash);
        assertNotBlank(hash);

        assertNotEquals(
                rawPassword,
                hash
        );
    }

    @Test
    void shouldMatchCorrectPassword() {

        // Given
        String rawPassword = "Password123!";

        String hash = hasher.hash(rawPassword);

        // When
        boolean result =
                hasher.matches(rawPassword, hash);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldRejectIncorrectPassword() {

        // Given
        String rawPassword = "Password123!";
        String wrongPassword = "WrongPassword123!";

        String hash = hasher.hash(rawPassword);

        // When
        boolean result =
                hasher.matches(wrongPassword, hash);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {

        // Given
        String password = "Password123!";

        // When
        String hash1 = hasher.hash(password);
        String hash2 = hasher.hash(password);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldGenerateValidBcryptHash() {

        // Given
        String password = "Password123!";

        // When
        String hash = hasher.hash(password);

        // Then
        assertTrue(hash.startsWith("$2"));

        assertTrue(
                hash.length() >= 50
        );
    }

    @Test
    void shouldReturnFalseForInvalidHash() {

        // Given
        String password = "Password123!";
        String invalidHash = "invalid-hash";

        // When
        boolean result = hasher.matches(
                password,
                invalidHash
        );

        // Then
        assertFalse(result);
    }

    private void assertNotBlank(String value) {
        assertNotNull(value);
        assertFalse(value.isBlank());
    }
}
