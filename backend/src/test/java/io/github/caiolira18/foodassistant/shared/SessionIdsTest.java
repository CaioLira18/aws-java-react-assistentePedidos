package io.github.caiolira18.foodassistant.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class SessionIdsTest {

    @Test
    void acceptsUuidsAndRejectsEverythingElse() {
        String uuid = UUID.randomUUID().toString();

        assertEquals(uuid, SessionIds.validate(uuid));
        assertThrows(IllegalArgumentException.class, () -> SessionIds.validate(null));
        assertThrows(IllegalArgumentException.class, () -> SessionIds.validate("curto"));
        assertThrows(IllegalArgumentException.class, () -> SessionIds.validate("sessão com acento"));
        assertThrows(IllegalArgumentException.class, () -> SessionIds.validate("a".repeat(65)));
    }
}