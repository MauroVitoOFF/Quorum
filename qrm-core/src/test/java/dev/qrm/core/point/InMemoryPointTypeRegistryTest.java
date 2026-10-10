package dev.qrm.core.point;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointType;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryPointTypeRegistryTest {
    private static final PointType DUTY = new PointType("duty", "k", Set.of(PointOwner.Kind.JOB), false, u -> {});

    @Test
    void sameDefinitionIsAllowedDifferentOneIsRejected() {
        InMemoryPointTypeRegistry r = new InMemoryPointTypeRegistry();
        r.register(DUTY);
        r.register(DUTY);
        assertEquals(1, r.all().size());
        assertTrue(r.find("duty").isPresent());
        assertThrows(IllegalArgumentException.class,
                () -> r.register(new PointType("duty", "other", Set.of(PointOwner.Kind.JOB), false, u -> {})));
    }

    @Test
    void invalidIdOrEmptyOwnersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PointType("Bad Id", "k", Set.of(PointOwner.Kind.JOB), false, u -> {}));
        assertThrows(IllegalArgumentException.class, () -> new PointType("ok", "k", Set.of(), false, u -> {}));
    }
}
