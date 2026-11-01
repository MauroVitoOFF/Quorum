package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PistolTimingsTest {
    @Test
    void ilServerTolleraUnTickInMenoDelClient() {
        assertEquals(4, PistolTimings.SERVER_FIRE_COOLDOWN_TICKS);
        assertFalse(PistolTimings.serverCanFire(100, 103));
        assertTrue(PistolTimings.serverCanFire(100, 104));
        // Il client resta a 5: due pressioni a 5 tick arrivate a 4 tick di distanza passano.
        assertFalse(PistolTimings.canFire(100, 104));
    }

    @Test
    void cooldownBoundary() {
        assertFalse(PistolTimings.canFire(100, 104));
        assertTrue(PistolTimings.canFire(100, 105));
        assertTrue(PistolTimings.canFire(100, 106));
        assertEquals(5, PistolTimings.FIRE_COOLDOWN_TICKS);
        assertEquals(6, PistolTimings.HOLD_THRESHOLD_TICKS);
        assertEquals(4, PistolTimings.AIM_TICKS);
    }

    @Test
    void blocksFireOnlyForDrawReloadsAndSlide() {
        Set<PistolAction> blocking = Set.of(PistolAction.DRAW, PistolAction.RELOAD_TACTICAL,
                PistolAction.RELOAD_EMPTY, PistolAction.SLIDE_RELEASE, PistolAction.SLIDE_RACK);
        for (PistolAction a : PistolAction.values()) {
            assertEquals(blocking.contains(a), a.blocksFire(), a.name());
        }
    }

    @Test
    void durateECommitSiRicavanoDalleClip() {
        assertTiming(PistolAction.DRAW, 16);
        assertTiming(PistolAction.RELOAD_TACTICAL, 50, Map.entry(16, CommitPoint.MAG_OUT),
                Map.entry(32, CommitPoint.MAG_IN));
        assertTiming(PistolAction.RELOAD_EMPTY, 55, Map.entry(16, CommitPoint.MAG_OUT),
                Map.entry(32, CommitPoint.MAG_IN), Map.entry(42, CommitPoint.SLIDE));
        assertTiming(PistolAction.SLIDE_RELEASE, 8, Map.entry(4, CommitPoint.SLIDE));
        assertTiming(PistolAction.SLIDE_RACK, 10, Map.entry(6, CommitPoint.SLIDE));
        assertTiming(PistolAction.AMMO_CHECK, 62, Map.entry(23, CommitPoint.CHECK));
        assertTiming(PistolAction.INSPECT, 84);
    }

    @SafeVarargs
    private static void assertTiming(PistolAction a, int ticks, Map.Entry<Integer, CommitPoint>... commits) {
        assertEquals(ticks, a.durationTicks(), a.name() + " durata");
        assertEquals(java.util.List.of(commits), a.commits(), a.name() + " commit");
        for (Map.Entry<Integer, CommitPoint> c : a.commits()) {
            assertTrue(c.getKey() <= a.durationTicks(), a.name() + ": commit oltre la fine");
        }
    }

    @Test
    void ilRegistroTrovaLArmaDalLOggetto() {
        assertEquals("police_pistol", WeaponDef.forItem("qrm_police:police_pistol").orElseThrow().id());
        assertTrue(WeaponDef.forItem("minecraft:stick").isEmpty());
        assertEquals(WeaponDef.pistol(), WeaponDef.byId("police_pistol").orElseThrow());
        assertTrue(WeaponDef.all().containsKey("police_pistol"));
    }

    @Test
    void laDefinizioneDellArmaHaTuttoCioCheServe() {
        WeaponDef d = WeaponDef.pistol();
        assertEquals("police_pistol", d.id());
        assertEquals(64.0, d.ballistics().range());
        assertEquals(0.3, d.ballistics().hipSpread());
        assertEquals("support", d.leftDefault());
        for (PistolAction a : PistolAction.values()) {
            assertTrue(d.action(a.clip()).isPresent(), a.name() + " non definita");
        }
        // ogni clip di un'azione esiste nel file di animazione
        for (PistolAction a : PistolAction.values()) {
            WeaponDef.Action def = d.action(a.clip()).orElseThrow();
            if (def.clip() != null) {
                assertTrue(d.clips().containsKey(def.clip()), a.name() + ": clip mancante");
            }
        }
    }
}
