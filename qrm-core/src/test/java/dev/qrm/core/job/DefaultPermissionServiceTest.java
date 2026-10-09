package dev.qrm.core.job;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.permission.PermissionChangedEvent;
import dev.qrm.api.permission.PermissionEffect;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPermissionServiceTest {
    private JobsEnv env;
    private DefaultPermissionService perms;
    private PlayerCharacter c;

    @BeforeEach
    void setUp() {
        env = new JobsEnv();
        perms = new DefaultPermissionService(env.db, env.bus, env.registry, env.jobs);
        c = env.newCharacter();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private void work(String job, String grade) {
        env.jobs.assign(c.id(), job, grade);
        env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
    }

    @Test
    void unemployedHasNothing() {
        assertFalse(perms.has(c.id(), "police.arrest"));
        assertEquals(Set.of(), perms.effective(c.id()));
    }

    @Test
    void gradeInheritsFromLowerGradesOnlyWhileOnDuty() {
        env.jobs.assign(c.id(), "police", "officer");
        assertFalse(perms.has(c.id(), "police.arrest"));              // fuori servizio
        env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
        assertTrue(perms.has(c.id(), "police.arrest"));
        assertTrue(perms.has(c.id(), "police.duty"));                  // ereditato da cadet
        assertFalse(perms.has(c.id(), "police.fine"));
        assertFalse(perms.has(c.id(), "mechanic.repair"));
        assertEquals(Set.of("police.arrest", "police.cuff", "police.duty"), perms.effective(c.id()));
    }

    @Test
    void lowerGradeDoesNotGetHigherGradePermissions() {
        work("police", "cadet");
        assertTrue(perms.has(c.id(), "police.duty"));
        assertFalse(perms.has(c.id(), "police.arrest"));
    }

    @Test
    void wildcardGradeCoversEverythingUnderThePrefix() {
        work("police", "chief");
        assertTrue(perms.has(c.id(), "police.anything.at.all"));
        assertFalse(perms.has(c.id(), "policeman.x"));
    }

    @Test
    void grantAndDenyOverridesApplyEvenOffDuty() {
        perms.grant(c.id(), "mechanic.*");
        assertTrue(perms.has(c.id(), "mechanic.repair"));
        perms.deny(c.id(), "mechanic.repair");
        assertFalse(perms.has(c.id(), "mechanic.repair"));
        assertTrue(perms.has(c.id(), "mechanic.paint"));
        perms.clear(c.id(), "mechanic.repair");
        assertTrue(perms.has(c.id(), "mechanic.repair"));
    }

    @Test
    void denyOverridesJobPermission() {
        work("police", "chief");
        perms.deny(c.id(), "police.arrest");
        assertFalse(perms.has(c.id(), "police.arrest"));
        assertTrue(perms.has(c.id(), "police.cuff"));
        perms.clear(c.id(), "police.arrest");
        assertTrue(perms.has(c.id(), "police.arrest"));
    }

    @Test
    void effectiveCombinesJobGrantsAndExactDenies() {
        work("police", "officer");
        perms.grant(c.id(), "mechanic.*");
        perms.deny(c.id(), "police.cuff");
        assertEquals(Set.of("police.arrest", "police.duty", "mechanic.*"), perms.effective(c.id()));
    }

    @Test
    void overridesOfListsOverridesSortedByNode() {
        perms.grant(c.id(), "b.x");
        perms.deny(c.id(), "a.*");
        assertEquals(List.of("a.*", "b.x"), List.copyOf(perms.overridesOf(c.id()).keySet()));
        assertEquals(Map.of("a.*", PermissionEffect.DENY, "b.x", PermissionEffect.GRANT), perms.overridesOf(c.id()));
    }

    @Test
    void archivedCharacterHasNoPermissionsAtAll() {
        work("police", "chief");
        perms.grant(c.id(), "x.y");
        env.chars.archive(c.playerId(), c.id());
        assertFalse(perms.has(c.id(), "police.arrest"));
        assertFalse(perms.has(c.id(), "x.y"));
        assertEquals(Set.of(), perms.effective(c.id()));
    }

    @Test
    void orphanedJobGivesNoJobPermissionsButOverridesRemain() {
        work("police", "chief");
        perms.grant(c.id(), "x.y");
        env.registry.remove("police");
        assertFalse(perms.has(c.id(), "police.arrest"));
        assertTrue(perms.has(c.id(), "x.y"));
    }

    @Test
    void invalidNodesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> perms.has(c.id(), "police.*"));
        assertThrows(IllegalArgumentException.class, () -> perms.has(c.id(), "Bad Node"));
        assertThrows(IllegalArgumentException.class, () -> perms.grant(c.id(), "Bad Node"));
        assertThrows(IllegalArgumentException.class, () -> perms.deny(c.id(), "a.*.b"));
        assertThrows(IllegalArgumentException.class, () -> perms.clear(c.id(), ""));
    }

    @Test
    void unknownCharacterWritesNothingAndFiresNothing() {
        CharacterId ghost = CharacterId.random();
        perms.grant(ghost, "x.y");
        assertEquals(Map.of(), perms.overridesOf(ghost));
        assertTrue(env.events.isEmpty());
        assertFalse(perms.has(ghost, "x.y"));
    }

    @Test
    void eventsAreFiredOnlyOnRealChanges() {
        perms.grant(c.id(), "x.y");
        perms.grant(c.id(), "x.y");            // invariato
        perms.deny(c.id(), "x.y");             // cambia effetto
        perms.clear(c.id(), "x.y");
        perms.clear(c.id(), "x.y");            // già rimosso
        assertEquals(List.of(
                new PermissionChangedEvent(c.id(), "x.y", PermissionEffect.GRANT),
                new PermissionChangedEvent(c.id(), "x.y", PermissionEffect.DENY),
                new PermissionChangedEvent(c.id(), "x.y", PermissionEffect.CLEARED)), env.events);
    }
}
