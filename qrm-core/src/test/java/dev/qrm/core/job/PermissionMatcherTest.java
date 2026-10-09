package dev.qrm.core.job;

import static dev.qrm.api.permission.PermissionEffect.DENY;
import static dev.qrm.api.permission.PermissionEffect.GRANT;
import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.permission.PermissionEffect;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionMatcherTest {
    private static final Map<String, PermissionEffect> NONE = Map.of();

    @Test
    void matching() {
        assertTrue(PermissionMatcher.matches("police.arrest", "police.arrest"));
        assertFalse(PermissionMatcher.matches("police.arrest", "police.cuff"));
        assertTrue(PermissionMatcher.matches("police.*", "police.arrest"));
        assertTrue(PermissionMatcher.matches("police.*", "police.cuff.hard"));
        assertFalse(PermissionMatcher.matches("police.*", "police"));
        assertFalse(PermissionMatcher.matches("police.*", "policeman.x"));
        assertTrue(PermissionMatcher.matches("*", "anything.at.all"));
    }

    @Test
    void specificityOrdering() {
        assertTrue(PermissionMatcher.specificity("police.arrest") > PermissionMatcher.specificity("police.cuff.*"));
        assertTrue(PermissionMatcher.specificity("police.cuff.*") > PermissionMatcher.specificity("police.*"));
        assertTrue(PermissionMatcher.specificity("police.*") > PermissionMatcher.specificity("*"));
    }

    @Test
    void jobGrantsOnlyWhenApplicable() {
        Set<String> patterns = Set.of("police.arrest", "mechanic.*");
        assertTrue(PermissionMatcher.has("police.arrest", true, patterns, NONE));
        assertTrue(PermissionMatcher.has("mechanic.repair", true, patterns, NONE));
        assertFalse(PermissionMatcher.has("police.cuff", true, patterns, NONE));
        assertFalse(PermissionMatcher.has("police.arrest", false, patterns, NONE));
    }

    @Test
    void overridesWorkEvenWhenJobDoesNotApply() {
        assertTrue(PermissionMatcher.has("a.b", false, Set.of(), Map.of("a.*", GRANT)));
        assertFalse(PermissionMatcher.has("a.b", false, Set.of(), NONE));
    }

    @Test
    void overrideBeatsJobNodes() {
        Set<String> job = Set.of("police.*");
        assertFalse(PermissionMatcher.has("police.arrest", true, job, Map.of("police.arrest", DENY)));
        // anche un DENY con jolly batte un nodo esatto del lavoro: gli override vincono sul lavoro
        assertFalse(PermissionMatcher.has("police.arrest", true, Set.of("police.arrest"), Map.of("police.*", DENY)));
        assertTrue(PermissionMatcher.has("mechanic.repair", true, Set.of(), Map.of("mechanic.repair", GRANT)));
    }

    @Test
    void mostSpecificOverrideWins() {
        Map<String, PermissionEffect> o = Map.of("police.*", DENY, "police.arrest", GRANT, "police.cuff.*", GRANT);
        assertTrue(PermissionMatcher.has("police.arrest", false, Set.of(), o));       // esatto GRANT batte jolly DENY
        assertFalse(PermissionMatcher.has("police.fine", false, Set.of(), o));        // solo police.* DENY
        assertTrue(PermissionMatcher.has("police.cuff.hard", false, Set.of(), o));    // police.cuff.* GRANT batte police.*
        assertEquals(Optional.of(DENY), PermissionMatcher.overrideEffect(o, "police.fine"));
        assertEquals(Optional.empty(), PermissionMatcher.overrideEffect(o, "mechanic.repair"));
    }

    @Test
    void starMatchesEverythingButIsLeastSpecific() {
        Map<String, PermissionEffect> o = Map.of("*", GRANT, "secret.*", DENY);
        assertTrue(PermissionMatcher.has("anything", false, Set.of(), o));
        assertFalse(PermissionMatcher.has("secret.door", false, Set.of(), o));
    }

    @Test
    void effectiveIsJobPlusGrantsMinusExactDenies() {
        Set<String> job = Set.of("police.arrest", "police.cuff");
        Map<String, PermissionEffect> o = Map.of("mechanic.*", GRANT, "police.cuff", DENY, "other.*", DENY);
        assertEquals(Set.of("police.arrest", "mechanic.*"), PermissionMatcher.effective(true, job, o));
        assertEquals(Set.of("mechanic.*"), PermissionMatcher.effective(false, job, o));
        assertEquals(Set.of(), PermissionMatcher.effective(false, job, NONE));
    }
}
