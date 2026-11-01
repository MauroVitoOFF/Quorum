package dev.qrm.api;

import dev.qrm.api.internal.ServiceRegistry;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QrmRegistryTest {
    @AfterEach
    void cleanup() {
        ServiceRegistry.clear();
    }

    @Test
    void getUnregisteredThrowsHelpfulMessage() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> QRM.get(String.class));
        assertTrue(e.getMessage().contains("QrmReadyEvent"));
    }

    @Test
    void registerAndGet() {
        ServiceRegistry.register(String.class, "hello");
        assertEquals("hello", QRM.get(String.class));
    }

    @Test
    void doubleRegistrationRejected() {
        ServiceRegistry.register(String.class, "a");
        assertThrows(IllegalStateException.class, () -> ServiceRegistry.register(String.class, "b"));
    }

    @Test
    void unregisterRemoves() {
        ServiceRegistry.register(String.class, "a");
        ServiceRegistry.unregister(String.class);
        assertThrows(IllegalStateException.class, () -> QRM.get(String.class));
    }
}
