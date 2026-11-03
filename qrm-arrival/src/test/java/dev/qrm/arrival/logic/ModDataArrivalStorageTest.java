package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.id.CharacterId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModDataArrivalStorageTest {
    final MemoryData data = new MemoryData();
    final CharacterId anna = new CharacterId(UUID.randomUUID());

    private ModDataArrivalStorage storageAt(String instant) {
        return new ModDataArrivalStorage(data, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    void markArrived_then_hasArrived_true() {
        ModDataArrivalStorage s = storageAt("2026-11-03T10:00:00Z");
        s.markArrived(anna);
        assertTrue(s.hasArrived(anna));
    }

    @Test
    void unknownCharacter_hasArrived_false() {
        assertFalse(storageAt("2026-11-03T10:00:00Z").hasArrived(anna));
    }

    @Test
    void installedAt_isWrittenOnceAndStable() {
        ModDataArrivalStorage first = storageAt("2026-11-03T10:00:00Z");
        ModDataArrivalStorage second = storageAt("2026-12-01T08:00:00Z");
        assertEquals(Instant.parse("2026-11-03T10:00:00Z"), first.installedAt());
        assertEquals(first.installedAt(), second.installedAt());
    }
}
