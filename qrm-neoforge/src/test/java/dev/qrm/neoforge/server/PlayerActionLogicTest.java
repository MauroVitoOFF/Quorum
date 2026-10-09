package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerActionLogicTest {
    @Test
    void gameModesAreParsedCaseInsensitively() {
        assertEquals(Optional.of("creative"), GameModes.parse("creative"));
        assertEquals(Optional.of("survival"), GameModes.parse("  SURVIVAL "));
        assertEquals(Optional.of("spectator"), GameModes.parse("Spectator"));
        assertEquals(Optional.of("adventure"), GameModes.parse("adventure"));
    }

    @Test
    void unknownGameModesAreRejected() {
        assertEquals(Optional.empty(), GameModes.parse("god"));
        assertEquals(Optional.empty(), GameModes.parse(""));
        assertEquals(Optional.empty(), GameModes.parse(null));
        assertEquals(Optional.empty(), GameModes.parse("0"));
    }

    @Test
    void freezeTogglesAndRemembersTheAnchor() {
        FreezeRegistry<String> reg = new FreezeRegistry<>();
        UUID id = UUID.randomUUID();
        FreezeRegistry.Anchor<String> a = new FreezeRegistry.Anchor<>("overworld", 1, 64, 2, 90f, 10f);
        assertFalse(reg.isFrozen(id));
        assertTrue(reg.toggle(id, a));
        assertTrue(reg.isFrozen(id));
        assertEquals(Optional.of(a), reg.anchorOf(id));
        assertFalse(reg.toggle(id, new FreezeRegistry.Anchor<>("nether", 0, 0, 0, 0f, 0f)));
        assertFalse(reg.isFrozen(id));
        assertTrue(reg.anchorOf(id).isEmpty());
    }

    @Test
    void freezeIsPerPlayerAndForgettable() {
        FreezeRegistry<String> reg = new FreezeRegistry<>();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        reg.toggle(a, new FreezeRegistry.Anchor<>("w", 0, 0, 0, 0f, 0f));
        assertFalse(reg.isFrozen(b));
        reg.forget(a);
        assertTrue(reg.isEmpty());
        reg.forget(a);                                    // idempotente
    }
}
