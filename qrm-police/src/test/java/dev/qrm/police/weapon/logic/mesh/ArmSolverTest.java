package dev.qrm.police.weapon.logic.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.police.weapon.logic.WeaponDef;
import dev.qrm.client.api.rig.BoneTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ArmSolverTest {
    private static final float EPS = 1e-4f;

    private static Map<String, ArmSolver.Target> targets() {
        return Map.of(
                "base", new ArmSolver.Target("b", new float[] {0, 0, 0}, new float[] {0, 0, 16}),
                "far", new ArmSolver.Target("b", new float[] {16, 0, 0}, new float[] {0, 16, 16}));
    }

    @Test
    void ilPugnoFinisceSulPuntoDiPresa() {
        ArmSolver.Hand hand = new ArmSolver.Hand(new Vector3f(0.5f, 0.3f, 0.7f), new Vector3f(1f, -1f, 2.5f));
        for (boolean right : new boolean[] {true, false}) {
            Matrix4f f = ArmSolver.frame(right, hand);
            // il pugno nel sistema del braccio: x = ±1/16, y = −8/16 rispetto al perno
            Vector3f fist = f.transformPosition(new Vector3f(right ? 1f : -1f, -8f, 0f).div(16f), new Vector3f());
            assertEquals(hand.fist().x, fist.x, EPS);
            assertEquals(hand.fist().y, fist.y, EPS);
            assertEquals(hand.fist().z, fist.z, EPS);
        }
    }

    @Test
    void ilBraccioPuntaDallaSpalla() {
        ArmSolver.Hand hand = new ArmSolver.Hand(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 2f));
        Matrix4f f = ArmSolver.frame(true, hand);
        // +Y del braccio va dal pugno verso la spalla (qui +Z)
        Vector3f y = f.transformDirection(new Vector3f(0, 1, 0), new Vector3f());
        assertEquals(0f, y.x, EPS);
        assertEquals(0f, y.y, EPS);
        assertEquals(1f, y.z, EPS);
        // la cornice è una rotazione pura (lunghezze invariate)
        assertEquals(1f, f.transformDirection(new Vector3f(1, 0, 0), new Vector3f()).length(), EPS);
    }

    @Test
    void senzaProgrammaLaManoStaSulPuntoDiBase() {
        ArmSolver.Hand h = ArmSolver.follow(targets(), "base", List.of(), 1.0, Map.of());
        assertEquals(0f, h.fist().x, EPS);
        assertEquals(1f, h.shoulder().z, EPS);
    }

    @Test
    void ilPassaggioScorreDalBaseAlDestinatario() {
        List<ArmSolver.Step> schedule = List.of(new ArmSolver.Step(1.0, 2.0, "far"));
        // prima dell'inizio: base
        assertEquals(0f, ArmSolver.follow(targets(), "base", schedule, 0.5, Map.of()).fist().x, EPS);
        // a metà la curva dolce vale 0,5
        assertEquals(0.5f, ArmSolver.follow(targets(), "base", schedule, 1.5, Map.of()).fist().x, EPS);
        // alla fine: sul punto di destinazione, con la sua spalla
        ArmSolver.Hand end = ArmSolver.follow(targets(), "base", schedule, 2.5, Map.of());
        assertEquals(1f, end.fist().x, EPS);
        assertEquals(1f, end.shoulder().y, EPS);
    }

    @Test
    void ilPuntoSegueIlBoneECiSiMuoveInsieme() {
        Matrix4f moved = new Matrix4f().translate(0f, 1f, 0f);
        ArmSolver.Hand h = ArmSolver.at(targets().get("far"), Map.of("b", moved));
        assertEquals(1f, h.fist().x, EPS);
        assertEquals(1f, h.fist().y, EPS);
    }

    @Test
    void laPosaSenzaScaleNonCollassaLaMano() {
        BoneTransform hidden = new BoneTransform(new float[] {0, 0, 0}, new float[] {0, 0, 0}, new float[] {0, 0, 0});
        Map<String, BoneTransform> pose = new HashMap<>();
        pose.put("magazine", hidden);
        BoneTransform t = ArmSolver.withoutScale(pose).get("magazine");
        assertEquals(1f, t.scale()[0], EPS);
        assertEquals(1f, t.scale()[1], EPS);
        assertEquals(1f, t.scale()[2], EPS);
    }

    @Test
    void ilProgrammaDellaPistolaUsaSoloPuntiDefiniti() {
        WeaponDef d = WeaponDef.pistol();
        assertTrue(d.handTargets().containsKey(d.leftDefault()));
        for (String clip : d.clips().keySet()) {
            for (ArmSolver.Step s : d.leftSchedule(clip)) {
                assertTrue(ArmSolver.FREEZE.equals(s.target()) || d.handTargets().containsKey(s.target()), clip);
                assertTrue(s.end() >= s.start(), clip);
            }
        }
        // ogni programma, di ciascuna mano, usa punti definiti e sta dentro la durata della sua clip
        for (String clip : d.clips().keySet()) {
            for (ArmSolver.Step s : d.rightSchedule(clip)) {
                assertTrue(ArmSolver.FREEZE.equals(s.target()) || d.handTargets().containsKey(s.target()), clip);
            }
            java.util.List<ArmSolver.Step> all = new java.util.ArrayList<>(d.leftSchedule(clip));
            all.addAll(d.rightSchedule(clip));
            for (ArmSolver.Step s : all) {
                assertTrue(s.end() <= d.clips().get(clip).length() + 1e-9, clip + " oltre la fine");
            }
        }
        // le tre clip del riempimento ci sono e restano nella posa finale (hold)
        for (String clip : new String[] {"refill_in", "refill_loop", "refill_out"}) {
            assertTrue(d.clips().get(clip).holdLast(), clip);
        }
    }

    @Test
    void ilCaricatoreImpugnatoHaClipProgrammiEPuntiCoerenti() {
        WeaponDef d = WeaponDef.byId("police_magazine").orElseThrow();
        assertEquals("qrm_police:police_magazine", d.item());
        assertTrue(!d.renderOnlyInActions());
        assertTrue(!d.showCrosshair());
        assertTrue(d.handTargets().containsKey(d.leftDefault()));
        assertTrue(d.handTargets().containsKey(d.rightDefault()));
        // a riposo la mano destra sta fuori dalla vista e l'idle e' l'alias di mag_idle
        assertEquals("right_away", d.rightDefault());
        assertEquals("mag_idle", d.clipAliases().get("idle"));
        assertTrue(d.clips().get("idle") == d.clips().get("mag_idle"));
        for (String clip : new String[] {"refill_in", "refill_loop", "refill_out"}) {
            assertTrue(d.clips().containsKey(clip), clip);
            for (ArmSolver.Step s : d.rightSchedule(clip)) {
                assertTrue(d.handTargets().containsKey(s.target()), clip + " " + s.target());
                assertTrue(s.end() <= d.clips().get(clip).length() + 1e-9, clip + " oltre la fine");
            }
        }
        // la pistola resta con il suo mirino e il suo disegno sempre attivo
        assertTrue(WeaponDef.pistol().showCrosshair());
        assertTrue(!WeaponDef.pistol().renderOnlyInActions());
    }
}
