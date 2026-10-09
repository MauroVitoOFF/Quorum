package dev.qrm.neoforge.client.theme;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.client.api.theme.ThemeFragments;
import dev.qrm.client.api.theme.ThemeManifest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ThemeFragmentTest {
    private static final String FRAGMENT = """
            {"version":1,
             "colors":{"bank.debt":"#FF0000","other.x":"#00FF00"},
             "metrics":{"bank.rowGap":4},
             "textures":{"bank.panel":{"id":"bank:textures/gui/p.png","size":[16,16],"slice":[4,4,4,4]},
                         "panel":{"id":"bank:textures/gui/q.png","size":[8,8]}}}""";

    private final ThemeManifest base = BuiltInTheme.manifest();

    @Test
    void mergeAddsTheModulesPrefixedKeys() {
        List<String> warnings = new ArrayList<>();
        ThemeManifest merged = base.withFragment("bank", ThemeManifest.parseBuiltIn(FRAGMENT), warnings);
        assertEquals(0xFFFF0000, merged.color("bank.debt"));
        assertEquals(4.0, merged.metric("bank.rowGap"));
        assertEquals(16, merged.texture("bank.panel").width());
        // le chiavi di QRM restano quelle di QRM
        assertEquals(base.texture("panel"), merged.texture("panel"));
        assertEquals(base.color("text"), merged.color("text"));
    }

    @Test
    void keysOutsideThePrefixAreDiscardedWithAWarning() {
        List<String> warnings = new ArrayList<>();
        ThemeManifest merged = base.withFragment("bank", ThemeManifest.parseBuiltIn(FRAGMENT), warnings);
        assertThrows(IllegalArgumentException.class, () -> merged.color("other.x"));
        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("other.x")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("'panel'")));
    }

    @Test
    void keysAlreadyDefinedAreDiscardedWithAWarning() {
        ThemeManifest once = base.withFragment("bank", ThemeManifest.parseBuiltIn(FRAGMENT), new ArrayList<>());
        List<String> warnings = new ArrayList<>();
        ThemeManifest twice = once.withFragment("bank", ThemeManifest.parseBuiltIn(FRAGMENT), warnings);
        assertEquals(once.color("bank.debt"), twice.color("bank.debt"));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("already defined")), warnings.toString());
    }

    @Test
    void aPackOverridesTheFragmentKeyByKeyAndInvalidEntriesFallBack() {
        ThemeManifest builtIn = ThemeManifest.parseBuiltIn("""
                {"version":1,"colors":{"bank.debt":"#FF0000","bank.ok":"#00FF00"},"metrics":{},"textures":{}}""");
        ThemeManifest.Result r = ThemeManifest.parse("""
                {"version":1,"colors":{"bank.debt":"#0000FF","bank.ok":"not a color"}}""", builtIn);
        assertEquals(0xFF0000FF, r.manifest().color("bank.debt"));
        assertEquals(0xFF00FF00, r.manifest().color("bank.ok"));
        assertEquals(1, r.warnings().size());
    }

    @Test
    void registryValidatesAndRejectsDuplicates() {
        Identifier res = Identifier.fromNamespaceAndPath("fragtest", "theme/qrm.json");
        ThemeFragments.register("fragtest", res);
        assertTrue(ThemeFragments.all().contains(new ThemeFragments.Fragment("fragtest", res)));
        assertThrows(IllegalStateException.class, () -> ThemeFragments.register("fragtest", res));
        assertThrows(IllegalArgumentException.class, () -> ThemeFragments.register("Bad-Id", res));
        assertThrows(IllegalArgumentException.class, () -> ThemeFragments.register("", res));
        assertThrows(IllegalArgumentException.class, () -> ThemeFragments.register("x".repeat(33), res));
    }
}
