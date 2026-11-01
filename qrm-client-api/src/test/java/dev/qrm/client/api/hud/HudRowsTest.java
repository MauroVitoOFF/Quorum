package dev.qrm.client.api.hud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class HudRowsTest {
    @Test
    void registeredRowCanBeFound() {
        HudRow row = () -> Optional.of(HudLine.of("ciao"));
        HudRows.register("hudtest:found", row);
        assertSame(row, HudRows.find("hudtest:found").orElseThrow());
        assertTrue(HudRows.find("hudtest:missing").isEmpty());
    }

    @Test
    void duplicateIdIsRejected() {
        HudRows.register("hudtest:dup", Optional::empty);
        assertThrows(IllegalStateException.class, () -> HudRows.register("hudtest:dup", Optional::empty));
    }

    @Test
    void malformedOrTooLongIdsAreRejected() {
        for (String id : new String[] {"senzaspazio", "Maiuscolo:x", "a:", ":b", "a b:c", "a:b c"}) {
            assertThrows(IllegalArgumentException.class, () -> HudRows.register(id, Optional::empty), id);
        }
        String tooLong = "hudtest:" + "x".repeat(HudRows.MAX_ID_LENGTH);
        assertThrows(IllegalArgumentException.class, () -> HudRows.register(tooLong, Optional::empty));
        assertThrows(NullPointerException.class, () -> HudRows.register(null, Optional::empty));
        assertThrows(NullPointerException.class, () -> HudRows.register("hudtest:nullrow", null));
    }

    @Test
    void lineDefaultsToTheTextColor() {
        assertEquals("text", HudLine.of("x").colorKey());
        assertEquals("text", new HudLine("x", null, "dot").colorKey());
        assertNull(HudLine.of("x").dotColorKey());
        assertThrows(NullPointerException.class, () -> new HudLine(null, "text", null));
    }
}
