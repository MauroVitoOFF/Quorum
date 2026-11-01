package dev.qrm.client.api.theme;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ThemeManifestTest {
    private static final String BUILT_IN = """
            {"version":1,
             "colors":{"text":"#112233","accent":"#80FFEEDD"},
             "metrics":{"pad":4,"scale":1.5},
             "textures":{"panel":{"id":"qrm:textures/gui/panel.png","size":[32,32],"slice":[4,4,4,4]},
                         "icon":{"id":"qrm:textures/gui/icon.png","size":[16,16]}}}""";

    private static ThemeManifest defaults() {
        return ThemeManifest.parseBuiltIn(BUILT_IN);
    }

    @Test
    void builtInReadsColorsMetricsAndTextures() {
        ThemeManifest m = defaults();
        assertEquals(0xFF112233, m.color("text"), "senza alfa diventa opaco");
        assertEquals(0x80FFEEDD, m.color("accent"), "con alfa resta com'è");
        assertEquals(4.0, m.metric("pad"));
        assertEquals(1.5, m.metric("scale"));
        ThemeManifest.Texture panel = m.texture("panel");
        assertEquals("qrm:textures/gui/panel.png", panel.id());
        assertEquals(new ThemeManifest.Slice(4, 4, 4, 4), panel.slice().orElseThrow());
        assertTrue(m.texture("icon").slice().isEmpty());
        assertEquals(Set.of("panel", "icon"), m.textureKeys());
    }

    @Test
    void unknownKeysThrow() {
        ThemeManifest m = defaults();
        assertThrows(IllegalArgumentException.class, () -> m.color("nope"));
        assertThrows(IllegalArgumentException.class, () -> m.metric("nope"));
        assertThrows(IllegalArgumentException.class, () -> m.texture("nope"));
    }

    @Test
    void builtInIsStrict() {
        assertThrows(IllegalStateException.class, () -> ThemeManifest.parseBuiltIn("{\"colors\":{\"text\":\"rosso\"}}"));
        assertThrows(IllegalStateException.class, () -> ThemeManifest.parseBuiltIn("non json"));
    }

    @Test
    void packOverridesOnlyWhatItDeclares() {
        ThemeManifest d = defaults();
        ThemeManifest.Result r = ThemeManifest.parse("{\"version\":1,\"colors\":{\"text\":\"#FF0000\"}}", d);
        assertFalse(r.usedFallback());
        assertTrue(r.warnings().isEmpty());
        assertEquals(0xFFFF0000, r.manifest().color("text"));
        assertEquals(d.color("accent"), r.manifest().color("accent"));
        assertEquals(4.0, r.manifest().metric("pad"));
    }

    @Test
    void invalidEntriesFallBackToDefaultsWithAWarning() {
        ThemeManifest d = defaults();
        String json = """
                {"version":1,
                 "colors":{"text":"#12345","accent":42},
                 "metrics":{"pad":-3,"scale":"grande"},
                 "textures":{"panel":{"id":"MAIUSCOLO:x","size":[32,32]},
                             "icon":{"id":"qrm:i.png","size":[99999,16]}}}""";
        ThemeManifest.Result r = ThemeManifest.parse(json, d);
        assertFalse(r.usedFallback());
        assertEquals(6, r.warnings().size(), r.warnings().toString());
        assertEquals(d.color("text"), r.manifest().color("text"));
        assertEquals(d.color("accent"), r.manifest().color("accent"));
        assertEquals(4.0, r.manifest().metric("pad"));
        assertEquals(1.5, r.manifest().metric("scale"));
        assertEquals(d.texture("panel"), r.manifest().texture("panel"));
        assertEquals(d.texture("icon"), r.manifest().texture("icon"));
    }

    @Test
    void sliceMustFitInsideTheTexture() {
        ThemeManifest d = defaults();
        String json = "{\"version\":1,\"textures\":{\"panel\":{\"id\":\"qrm:p.png\",\"size\":[8,8],\"slice\":[4,4,4,4]}}}";
        ThemeManifest.Result r = ThemeManifest.parse(json, d);
        assertEquals(1, r.warnings().size());
        assertEquals(d.texture("panel"), r.manifest().texture("panel"));
    }

    @Test
    void brokenJsonOrUnknownVersionUsesAllDefaults() {
        ThemeManifest d = defaults();
        for (String json : List.of("{rotto", "[1,2]", "{\"colors\":{}}", "{\"version\":2}", "{\"version\":\"1\"}")) {
            ThemeManifest.Result r = ThemeManifest.parse(json, d);
            assertTrue(r.usedFallback(), json);
            assertSame(d, r.manifest(), json);
            assertFalse(r.warnings().isEmpty(), json);
        }
    }

    @Test
    void fragmentKeysMustCarryTheModulePrefixAndCannotOverride() {
        ThemeManifest base = defaults();
        ThemeManifest fragment = ThemeManifest.parseBuiltIn("""
                {"version":1,
                 "colors":{"bank.card":"#00FF00","text":"#FFFFFF"},
                 "metrics":{"bank.gap":2},
                 "textures":{"bank.logo":{"id":"qrm_bank:l.png","size":[8,8]}}}""");
        List<String> warnings = new ArrayList<>();

        ThemeManifest merged = base.withFragment("bank", fragment, warnings);

        assertEquals(0xFF00FF00, merged.color("bank.card"));
        assertEquals(2.0, merged.metric("bank.gap"));
        assertEquals("qrm_bank:l.png", merged.texture("bank.logo").id());
        assertEquals(base.color("text"), merged.color("text"), "una chiave fuori prefisso non sovrascrive");
        assertEquals(1, warnings.size());
        assertThrows(IllegalArgumentException.class, () -> base.color("bank.card"), "il manifesto di partenza non cambia");

        List<String> again = new ArrayList<>();
        merged.withFragment("bank", fragment, again);
        assertTrue(again.stream().anyMatch(w -> w.contains("already defined")));
    }
}
