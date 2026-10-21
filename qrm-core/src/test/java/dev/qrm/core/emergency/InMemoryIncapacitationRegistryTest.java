package dev.qrm.core.emergency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryIncapacitationRegistryTest {
    @Test
    void noChecksMeansNobodyIsDown() {
        assertFalse(new InMemoryIncapacitationRegistry().isIncapacitated(UUID.randomUUID()));
    }

    @Test
    void checkAppliesOnlyToItsPlayer() {
        InMemoryIncapacitationRegistry r = new InMemoryIncapacitationRegistry();
        UUID down = UUID.randomUUID();
        r.register("m", down::equals);
        assertTrue(r.isIncapacitated(down));
        assertFalse(r.isIncapacitated(UUID.randomUUID()));
    }

    @Test
    void anyTrueCheckWins() {
        InMemoryIncapacitationRegistry r = new InMemoryIncapacitationRegistry();
        UUID id = UUID.randomUUID();
        r.register("a", u -> false);
        r.register("b", id::equals);
        assertTrue(r.isIncapacitated(id));
    }

    @Test
    void duplicateModuleIdIsRejected() {
        InMemoryIncapacitationRegistry r = new InMemoryIncapacitationRegistry();
        r.register("m", u -> false);
        assertThrows(IllegalArgumentException.class, () -> r.register("m", u -> true));
    }
}
