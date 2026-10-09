package dev.qrm.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QrmRegistryTest {
    @AfterEach
    void cleanup() {
        QRM.clear();
    }

    @Test
    void getUnregisteredThrowsHelpfulMessage() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> QRM.get(String.class));
        assertTrue(e.getMessage().contains("QrmReadyEvent"));
    }

    @Test
    void registerAndGet() {
        QRM.register(String.class, "hello");
        assertEquals("hello", QRM.get(String.class));
    }

    @Test
    void doubleRegistrationRejected() {
        QRM.register(String.class, "a");
        assertThrows(IllegalStateException.class, () -> QRM.register(String.class, "b"));
    }

    @Test
    void unregisterRemoves() {
        QRM.register(String.class, "a");
        QRM.unregister(String.class);
        assertThrows(IllegalStateException.class, () -> QRM.get(String.class));
    }
}
