package org.fincore.identity.user.application.usecase;


import org.fincore.identity.user.application.service.UsernameNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsernameNormalizerTest {

    private final UsernameNormalizer normalizer =
            new UsernameNormalizer();

    @Test
    void shouldTrimUsername() {

        String result =
                normalizer.normalize("  john  ");

        assertEquals("john", result);
    }

    @Test
    void shouldConvertUsernameToLowerCase() {

        String result =
                normalizer.normalize("JOHN");

        assertEquals("john", result);
    }

    @Test
    void shouldTrimAndLowercaseUsername() {

        String result =
                normalizer.normalize("  JoHn  ");

        assertEquals("john", result);
    }

    @Test
    void shouldReturnNullWhenUsernameIsNull() {

        assertNull(
                normalizer.normalize(null)
        );
    }
}