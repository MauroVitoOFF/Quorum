package dev.qrm.neoforge.client.theme;

import static org.junit.jupiter.api.Assertions.*;
import dev.qrm.client.api.theme.ThemeManifest;

import org.junit.jupiter.api.Test;

class ThemeHudKeysTest {
    @Test
    void builtInThemeHasTheHudAndHubKeys() {
        ThemeManifest t = BuiltInTheme.manifest();
        assertEquals(32, t.texture("hud.panel").width());
        assertEquals(32, t.texture("hud.panel").height());
        assertTrue(t.texture("hud.panel").slice().isPresent());
        assertEquals(40, t.texture("hub.tab").width());
        assertEquals(16, t.texture("hub.tab.selected").height());
        assertEquals(150.0, t.metric("hudMaxWidth"));
        assertEquals(0xFF5EE08A, t.color("statusOn"));
        assertEquals(0xFFFF6B6B, t.color("statusOff"));
    }
}
