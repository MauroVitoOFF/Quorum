package dev.qrm.neoforge.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HudVisibilityTest {
    @Test
    void visibleOnlyWhenEverythingAllowsIt() {
        assertTrue(HudVisibility.visible(true, false, true, true));
    }

    @Test
    void hiddenWhenAScreenIsOpen() {
        assertFalse(HudVisibility.visible(false, false, true, true));
    }

    @Test
    void hiddenWhenTheUiIsHiddenWithF1() {
        assertFalse(HudVisibility.visible(true, true, true, true));
    }

    @Test
    void hiddenWhenTheToggleIsOff() {
        assertFalse(HudVisibility.visible(true, false, false, true));
    }

    @Test
    void hiddenWithoutAnActiveCharacter() {
        assertFalse(HudVisibility.visible(true, false, true, false));
    }
}
