package dev.qrm.neoforge.client;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.client.api.hud.HudLine;
import dev.qrm.client.api.hud.HudRow;
import dev.qrm.client.api.hud.HudRows;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HudRowsTest {
    @Test
    void registersAndFindsARow() {
        HudRow row = () -> Optional.of(HudLine.of("Debito: 5"));
        HudRows.register("hudtest:debt", row);
        assertSame(row, HudRows.find("hudtest:debt").orElseThrow());
        assertEquals(Optional.empty(), HudRows.find("hudtest:missing"));
    }

    @Test
    void rejectsDuplicateAndMalformedIds() {
        HudRow row = Optional::empty;
        HudRows.register("hudtest:dup", row);
        assertThrows(IllegalStateException.class, () -> HudRows.register("hudtest:dup", row));
        for (String bad : new String[] {"name", "NoColon", "Upper:case", "a:", ":b", "a b:c", ""}) {
            assertThrows(IllegalArgumentException.class, () -> HudRows.register(bad, row), bad);
        }
        assertThrows(IllegalArgumentException.class,
                () -> HudRows.register("x:" + "y".repeat(HudRows.MAX_ID_LENGTH), row));
    }

    @Test
    void hudLineDefaultsToTheTextColorAndNoDot() {
        HudLine plain = HudLine.of("ciao");
        assertEquals("text", plain.colorKey());
        assertNull(plain.dotColorKey());
        assertEquals("text", new HudLine("x", null, "statusOn").colorKey());
    }
}
