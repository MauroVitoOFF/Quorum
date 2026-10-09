package dev.qrm.neoforge.client.theme;

import static org.junit.jupiter.api.Assertions.*;
import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.client.api.theme.ThemeManifest;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ThemeManifestTest {
    private final ThemeManifest defaults = BuiltInTheme.manifest();

    @Test
    void builtInHasAllKeys() {
        assertEquals(0xFF3EC9E6, defaults.color("accent"));
        assertEquals(0xA00F141B, defaults.color("scrim"));
        assertEquals(8.0, defaults.metric("padding"));
        assertEquals(0.82, defaults.metric("carouselScale"));
        ThemeManifest.Texture card = defaults.texture("character.card");
        assertEquals("qrm:textures/gui/character/card.png", card.id());
        assertEquals(120, card.width());
        assertEquals(160, card.height());
        assertEquals(Optional.of(new ThemeManifest.Slice(8, 8, 8, 8)), card.slice());
        assertTrue(defaults.texture("character.avatar").slice().isEmpty());
        assertTrue(defaults.textureKeys().contains("icon.lock"));
    }

    @Test
    void unknownKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> defaults.color("nope"));
        assertThrows(IllegalArgumentException.class, () -> defaults.metric("nope"));
        assertThrows(IllegalArgumentException.class, () -> defaults.texture("nope"));
    }

    @Test
    void partialPackOverridesOnlyWhatItDeclares() {
        String json = """
            {"version":1,"colors":{"accent":"#112233","scrim":"#80102030"},"metrics":{"padding":12}}""";
        ThemeManifest.Result r = ThemeManifest.parse(json, defaults);
        assertFalse(r.usedFallback());
        assertEquals(List.of(), r.warnings());
        assertEquals(0xFF112233, r.manifest().color("accent"));
        assertEquals(0x80102030, r.manifest().color("scrim"));
        assertEquals(12.0, r.manifest().metric("padding"));
        assertEquals(defaults.color("text"), r.manifest().color("text"));
        assertEquals(defaults.texture("panel"), r.manifest().texture("panel"));
    }

    @Test
    void packCanReplaceATexture() {
        String json = """
            {"version":1,"textures":{"panel":{"id":"mypack:textures/p.png","size":[64,64],"slice":[8,8,8,8]}}}""";
        ThemeManifest.Result r = ThemeManifest.parse(json, defaults);
        assertEquals("mypack:textures/p.png", r.manifest().texture("panel").id());
        assertEquals(64, r.manifest().texture("panel").width());
        assertEquals(defaults.texture("button.normal"), r.manifest().texture("button.normal"));
    }

    @Test
    void invalidEntriesFallBackPerKeyWithWarnings() {
        String json = """
            {"version":1,
             "colors":{"accent":"blue","text":"#12345"},
             "metrics":{"padding":-3,"cardWidth":"wide"},
             "textures":{
               "panel":{"id":"BAD ID","size":[48,48]},
               "button.normal":{"id":"qrm:textures/gui/common/button_normal.png","size":[32,20],"slice":[20,4,20,4]},
               "icon.play":{"id":"qrm:textures/gui/common/icon_play.png","size":[0,16]}}}""";
        ThemeManifest.Result r = ThemeManifest.parse(json, defaults);
        assertFalse(r.usedFallback());
        assertEquals(7, r.warnings().size(), r.warnings().toString());
        assertEquals(defaults.color("accent"), r.manifest().color("accent"));
        assertEquals(defaults.color("text"), r.manifest().color("text"));
        assertEquals(defaults.metric("padding"), r.manifest().metric("padding"));
        assertEquals(defaults.metric("cardWidth"), r.manifest().metric("cardWidth"));
        assertEquals(defaults.texture("panel"), r.manifest().texture("panel"));
        assertEquals(defaults.texture("button.normal"), r.manifest().texture("button.normal"));
        assertEquals(defaults.texture("icon.play"), r.manifest().texture("icon.play"));
    }

    private ThemeManifest.Result panel(String sizeAndSlice) {
        return ThemeManifest.parse("{\"version\":1,\"textures\":{\"panel\":{\"id\":\"qrm:textures/gui/common/panel.png\","
                + sizeAndSlice + "}}}", defaults);
    }

    @Test
    void sliceOverflowingIntIsRejected() {
        ThemeManifest.Result r = panel("\"size\":[48,48],\"slice\":[2147483647,0,2147483647,0]");
        assertEquals(1, r.warnings().size(), r.warnings().toString());
        assertEquals(defaults.texture("panel"), r.manifest().texture("panel"));
    }

    @Test
    void hugeSliceValueIsRejected() {
        ThemeManifest.Result r = panel("\"size\":[48,48],\"slice\":[10000000000,0,0,0]");
        assertEquals(1, r.warnings().size(), r.warnings().toString());
        assertEquals(defaults.texture("panel"), r.manifest().texture("panel"));
    }

    @Test
    void hugeSizeIsRejected() {
        String json = "{\"version\":1,\"textures\":{\"icon.play\":{\"id\":\"qrm:textures/gui/common/icon_play.png\",\"size\":[1e10,16]}}}";
        ThemeManifest.Result r = ThemeManifest.parse(json, defaults);
        assertEquals(1, r.warnings().size(), r.warnings().toString());
        assertEquals(defaults.texture("icon.play"), r.manifest().texture("icon.play"));
    }

    @Test
    void validSliceIsAccepted() {
        ThemeManifest.Result r = panel("\"size\":[48,48],\"slice\":[6,6,6,6]");
        assertEquals(List.of(), r.warnings());
        assertEquals("qrm:textures/gui/common/panel.png", r.manifest().texture("panel").id());
    }

    @Test
    void brokenJsonOrWrongVersionUsesTheFallback() {
        for (String json : new String[] {"{not json", "[]", "", "{\"version\":2}", "{\"colors\":{}}"}) {
            ThemeManifest.Result r = ThemeManifest.parse(json, defaults);
            assertTrue(r.usedFallback(), json);
            assertSame(defaults, r.manifest(), json);
            assertFalse(r.warnings().isEmpty(), json);
        }
    }

    @Test
    void currentThemeStartsAsBuiltInAndCanBeReplaced() {
        QrmTheme.set(defaults);
        assertSame(defaults, QrmTheme.current());
        ThemeManifest other = ThemeManifest.parse("{\"version\":1,\"colors\":{\"accent\":\"#000000\"}}", defaults).manifest();
        QrmTheme.set(other);
        assertSame(other, QrmTheme.current());
        QrmTheme.set(defaults);
    }
}
