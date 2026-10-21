package dev.qrm.police.weapon.logic.anim;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnimMixerTest {

    private static final float EPS = 1e-4f;

    /** Clip con un solo osso e un canale di posizione X lineare tra due keyframe. */
    private static AnimClip clip(double length, boolean loop, boolean holdLast, String bone, double t0, float x0,
            double t1, float x1, List<AnimEvent> events) {
        AnimChannel pos = new AnimChannel(List.of(new AnimKey(t0, new float[] {x0, 0, 0}, AnimKey.Interp.LINEAR),
                new AnimKey(t1, new float[] {x1, 0, 0}, AnimKey.Interp.LINEAR)));
        return new AnimClip(length, loop, holdLast, Map.of(bone, new AnimClip.Bone(pos, null, null)), events);
    }

    private static AnimClip constant(double length, boolean loop, boolean holdLast, String bone, float x) {
        return clip(length, loop, holdLast, bone, 0, x, length, x, List.of());
    }

    private static float x(Map<String, BoneTransform> pose, String bone) {
        BoneTransform b = pose.get(bone);
        return b == null ? 0f : b.position()[0];
    }

    private static AnimMixer mixer() {
        return new AnimMixer(4, new float[] {-10, 2, 3});
    }

    // --- tempo delle clip ---

    @Test
    void clipInLoopRiavvolgeIlTempo() {
        AnimClip c = constant(2, true, false, "root", 0);
        assertEquals(0.5, AnimMixer.clipTime(c, 2.5), 1e-9);
        assertEquals(1.0, AnimMixer.clipTime(c, 5.0), 1e-9);
    }

    @Test
    void clipSenzaLoopSiFermaAllaFine() {
        AnimClip c = constant(2, false, false, "root", 0);
        assertEquals(2.0, AnimMixer.clipTime(c, 3.7), 1e-9);
        assertEquals(1.2, AnimMixer.clipTime(c, 1.2), 1e-9);
        assertEquals(0.0, AnimMixer.clipTime(c, -1), 1e-9);
    }

    @Test
    void clipInLoopDiDurataNullaNonDivideComunquePerZero() {
        AnimClip c = constant(0, true, false, "root", 0);
        assertEquals(0.0, AnimMixer.clipTime(c, 3), 1e-9);
    }

    @Test
    void dissolvenzaLineareTraZeroEUno() {
        assertEquals(0f, AnimMixer.ramp(0, 2), EPS);
        assertEquals(0.5f, AnimMixer.ramp(1, 2), EPS);
        assertEquals(1f, AnimMixer.ramp(5, 2), EPS);
        assertEquals(0f, AnimMixer.ramp(-1, 2), EPS);
        assertEquals(1f, AnimMixer.ramp(0, 0), EPS);
    }

    // --- strato base ---

    @Test
    void idleInLoopCampionatoConIlTickParziale() {
        Map<String, AnimClip> clips = Map.of("idle", clip(1, true, false, "root", 0, 0, 1, 20, List.of()));
        AnimMixer m = mixer();
        for (int i = 0; i < 5; i++) m.tick(clips);
        // 5 tick + 0,5 = 5,5 tick = 0,275 s → x = 5,5
        assertEquals(5.5f, x(m.pose(clips, 0.5f, AnimMixer.State.NONE), "root"), EPS);
        for (int i = 0; i < 20; i++) m.tick(clips);
        // 25 tick = 1,25 s → riavvolto a 0,25 s → x = 5
        assertEquals(5f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
    }

    @Test
    void corsaPassaPerSprintInPoiSprintPoiSprintOutPoiIdle() {
        Map<String, AnimClip> clips = Map.of(
                "idle", constant(1, true, false, "root", 0),
                "sprint_in", constant(0.2, false, true, "root", 5), // 4 tick
                "sprint", constant(1, true, false, "root", 10),
                "sprint_out", constant(0.2, false, true, "root", 5));
        AnimMixer m = mixer();
        m.setSprinting(true);
        assertEquals("sprint_in", m.baseClip());
        for (int i = 0; i < 4; i++) m.tick(clips);
        assertEquals("sprint", m.baseClip());
        for (int i = 0; i < 10; i++) m.tick(clips);
        assertEquals(10f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
        m.setSprinting(false);
        assertEquals("sprint_out", m.baseClip());
        for (int i = 0; i < 4; i++) m.tick(clips);
        assertEquals("idle", m.baseClip());
    }

    @Test
    void transizioniDiCorsaMancantiVengonoSaltate() {
        Map<String, AnimClip> clips = Map.of(
                "idle", constant(1, true, false, "root", 0),
                "sprint", constant(1, true, false, "root", 10));
        AnimMixer m = mixer();
        m.setSprinting(true);
        m.tick(clips);
        assertEquals("sprint", m.baseClip());
        m.setSprinting(false);
        m.tick(clips);
        assertEquals("idle", m.baseClip());
    }

    @Test
    void cambioDelloStratoBaseSiDissolveInDueTick() {
        Map<String, AnimClip> clips = Map.of(
                "idle", constant(1, true, false, "root", 0),
                "sprint", constant(1, true, false, "root", 10));
        AnimMixer m = mixer();
        m.setSprinting(true);
        m.tick(clips); // sprint_in assente: passa a sprint in questo tick
        // Il cambio è avvenuto al tick 1: a 1 tick dal cambio metà strada.
        m.tick(clips);
        assertEquals(5f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
        m.tick(clips);
        assertEquals(10f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
    }

    // --- mira ---

    @Test
    void miraSiFondeInAimTicksConLOffsetStatico() {
        Map<String, AnimClip> clips = Map.of();
        AnimMixer m = mixer();
        assertEquals(0f, m.aimWeight(0f), EPS);
        m.setAiming(true);
        m.tick(clips);
        m.tick(clips);
        assertEquals(0.5f, m.aimWeight(0f), EPS);
        assertEquals(0.625f, m.aimWeight(0.5f), EPS);
        assertEquals(-5f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
        m.tick(clips);
        m.tick(clips);
        m.tick(clips);
        assertEquals(1f, m.aimWeight(0f), EPS);
        assertEquals(-10f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
        m.setAiming(false);
        m.tick(clips);
        assertEquals(0.75f, m.aimWeight(0f), EPS);
    }

    @Test
    void conUnaClipAimLaMiraUsaQuellaInveceDellOffset() {
        Map<String, AnimClip> clips = Map.of(
                "idle", constant(1, true, false, "root", 0),
                "aim", constant(1, true, false, "root", 8));
        AnimMixer m = mixer();
        m.setAiming(true);
        for (int i = 0; i < 4; i++) m.tick(clips);
        assertEquals(8f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
    }

    @Test
    void lOffsetDiMiraRestaAncheDuranteUnAzione() {
        Map<String, AnimClip> clips = Map.of("fire", constant(1, false, false, "root", 1));
        AnimMixer m = mixer();
        m.setAiming(true);
        for (int i = 0; i < 4; i++) m.tick(clips);
        m.play("fire", clips);
        m.tick(clips);
        m.tick(clips);
        // azione a peso pieno (x = 1) più l'offset di mira (-10)
        assertEquals(-9f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "root"), EPS);
    }

    // --- strato azione ---

    @Test
    void azioneEntraInDueTickEdEsceInDueTickDopoLaFine() {
        Map<String, AnimClip> clips = Map.of(
                "idle", constant(1, true, false, "slide", 0),
                "fire", constant(0.5, false, false, "slide", 4)); // 10 tick
        AnimMixer m = mixer();
        m.play("fire", clips);
        assertEquals(0f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
        m.tick(clips);
        assertEquals(2f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
        m.tick(clips);
        assertEquals(4f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
        for (int i = 0; i < 9; i++) m.tick(clips); // tick 11: 1 tick dopo la fine
        assertEquals(2f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
        m.tick(clips);
        assertEquals(0f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
        assertNull(m.actionClip());
    }

    @Test
    void azioneHoldLastTieneLUltimaPosa() {
        Map<String, AnimClip> clips = Map.of("jam", clip(0.5, false, true, "slide", 0, 0, 0.5, 6, List.of()));
        AnimMixer m = mixer();
        m.play("jam", clips);
        for (int i = 0; i < 40; i++) m.tick(clips);
        assertEquals("jam", m.actionClip());
        assertEquals(6f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
    }

    @Test
    void stopDissolveLAzioneInDueTick() {
        Map<String, AnimClip> clips = Map.of("reload", constant(3, false, false, "magazine", 8));
        AnimMixer m = mixer();
        m.play("reload", clips);
        for (int i = 0; i < 5; i++) m.tick(clips);
        m.stop();
        assertEquals(8f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "magazine"), EPS);
        m.tick(clips);
        assertEquals(4f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "magazine"), EPS);
        m.tick(clips);
        assertNull(m.actionClip());
    }

    @Test
    void lAzioneSovrascriveSoloGliOssiCheAnima() {
        Map<String, AnimClip> clips = Map.of(
                "idle", constant(1, true, false, "root", 3),
                "fire", constant(1, false, false, "slide", 4));
        AnimMixer m = mixer();
        m.play("fire", clips);
        m.tick(clips);
        m.tick(clips);
        Map<String, BoneTransform> pose = m.pose(clips, 0f, AnimMixer.State.NONE);
        assertEquals(3f, x(pose, "root"), EPS);
        assertEquals(4f, x(pose, "slide"), EPS);
    }

    @Test
    void unaNuovaAzioneSiFondeDaQuellaPrecedente() {
        Map<String, AnimClip> clips = Map.of(
                "a", constant(2, false, false, "slide", 10),
                "b", constant(2, false, false, "slide", 0));
        AnimMixer m = mixer();
        m.play("a", clips);
        m.tick(clips);
        m.tick(clips);
        m.play("b", clips);
        assertEquals(10f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
        m.tick(clips);
        assertEquals(5f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), 0.01f);
        m.tick(clips);
        assertEquals(0f, x(m.pose(clips, 0f, AnimMixer.State.NONE), "slide"), EPS);
    }

    @Test
    void clipSconosciutaNonFaNulla() {
        AnimMixer m = mixer();
        m.play("nope", Map.of());
        assertNull(m.actionClip());
        assertTrue(m.drainEvents().isEmpty());
    }

    // --- eventi ---

    @Test
    void eventiEmessiUnaVoltaSola() {
        List<AnimEvent> ev = List.of(
                new AnimEvent(0, "sound", List.of("pistol.draw")),
                new AnimEvent(0.12, "sound", List.of("pistol.draw_settle")),
                new AnimEvent(0.5, "commit", List.of("x")));
        Map<String, AnimClip> clips = Map.of("draw", clip(0.5, false, false, "root", 0, 0, 0.5, 1, ev));
        AnimMixer m = mixer();
        m.play("draw", clips);
        assertEquals(List.of("pistol.draw"), m.drainEvents().stream().map(e -> e.args().get(0)).toList());
        assertTrue(m.drainEvents().isEmpty());
        m.tick(clips); // 0,05 s
        m.tick(clips); // 0,10 s
        assertTrue(m.drainEvents().isEmpty());
        m.tick(clips); // 0,15 s
        assertEquals(1, m.drainEvents().size());
        for (int i = 0; i < 20; i++) m.tick(clips);
        List<AnimEvent> rest = m.drainEvents();
        assertEquals(1, rest.size());
        assertEquals("commit", rest.get(0).type());
    }

    @Test
    void dopoStopNonArrivanoAltriEventi() {
        List<AnimEvent> ev = List.of(new AnimEvent(0.5, "sound", List.of("pistol.mag_in")));
        Map<String, AnimClip> clips = Map.of("reload", clip(1, false, false, "root", 0, 0, 1, 1, ev));
        AnimMixer m = mixer();
        m.play("reload", clips);
        m.tick(clips);
        m.stop();
        for (int i = 0; i < 30; i++) m.tick(clips);
        assertTrue(m.drainEvents().isEmpty());
    }

    // --- sovrapposizione dello stato ---

    private static Map<String, AnimClip> stateClips() {
        Map<String, AnimClip> c = new HashMap<>();
        c.put("idle", constant(1, true, false, "root", 0));
        Map<String, AnimClip.Bone> bones = Map.of(
                "slide", new AnimClip.Bone(new AnimChannel(List.of(
                        new AnimKey(0, new float[] {0, 0, 0}, AnimKey.Interp.LINEAR),
                        new AnimKey(0.5, new float[] {0, 0, 2.5f}, AnimKey.Interp.LINEAR))), null, null),
                "port_cover", new AnimClip.Bone(null, null, new AnimChannel(List.of(
                        new AnimKey(0.5, new float[] {0, 1, 1}, AnimKey.Interp.LINEAR)))));
        c.put("fire_last", new AnimClip(0.5, false, true, bones, List.of()));
        c.put("reload", constant(2, false, false, "magazine", 7));
        return c;
    }

    @Test
    void carrelloBloccatoUsaLaPosaFinaleDiFireLast() {
        Map<String, AnimClip> clips = stateClips();
        AnimMixer m = mixer();
        m.tick(clips);
        Map<String, BoneTransform> pose = m.pose(clips, 0f, new AnimMixer.State(true, true));
        assertEquals(2.5f, pose.get("slide").position()[2], EPS);
        assertEquals(0f, pose.get("port_cover").scale()[0], EPS);
        assertFalse(pose.containsKey("magazine"));
    }

    @Test
    void senzaCaricatoreIlCaricatoreHaScalaZero() {
        Map<String, AnimClip> clips = stateClips();
        AnimMixer m = mixer();
        m.tick(clips);
        Map<String, BoneTransform> pose = m.pose(clips, 0f, new AnimMixer.State(false, false));
        assertArrayEquals(new float[] {0, 0, 0}, pose.get("magazine").scale(), EPS);
        assertFalse(pose.containsKey("slide"));
    }

    @Test
    void unAzioneInCorsoPrevaleSulloStato() {
        Map<String, AnimClip> clips = stateClips();
        AnimMixer m = mixer();
        m.play("reload", clips);
        m.tick(clips);
        m.tick(clips);
        AnimMixer.State noMag = new AnimMixer.State(false, false);
        assertEquals(0f, m.stateWeight(clips, 0f), EPS);
        BoneTransform mag = m.pose(clips, 0f, noMag).get("magazine");
        assertEquals(7f, mag.position()[0], EPS);
        assertArrayEquals(new float[] {1, 1, 1}, mag.scale(), EPS);
        // A metà dissolvenza d'ingresso lo stato pesa metà.
        AnimMixer m2 = mixer();
        m2.play("reload", clips);
        m2.tick(clips);
        assertEquals(0.5f, m2.stateWeight(clips, 0f), EPS);
    }

    @Test
    void finitaUnaClipHoldLastLoStatoPrendeIlSopravvento() {
        Map<String, AnimClip> clips = stateClips();
        AnimMixer m = mixer();
        m.play("fire_last", clips);
        for (int i = 0; i < 5; i++) m.tick(clips);
        assertEquals(0f, m.stateWeight(clips, 0f), EPS);
        for (int i = 0; i < 10; i++) m.tick(clips);
        assertEquals("fire_last", m.actionClip());
        assertEquals(1f, m.stateWeight(clips, 0f), EPS);
        // Lo stato dice che il carrello è tornato avanti (nessun blocco): la posa tenuta della clip resta valida,
        // perché la sovrapposizione tocca gli ossi solo quando c'è qualcosa da imporre.
        Map<String, BoneTransform> pose = m.pose(clips, 0f, new AnimMixer.State(true, true));
        assertEquals(2.5f, pose.get("slide").position()[2], EPS);
    }

    @Test
    void resetAzzeraAzioneCorsaEMira() {
        Map<String, AnimClip> clips = stateClips();
        AnimMixer m = mixer();
        m.play("reload", clips);
        m.setAiming(true);
        m.setSprinting(true);
        m.tick(clips);
        m.reset();
        assertNull(m.actionClip());
        assertEquals("idle", m.baseClip());
        assertEquals(0f, m.aimWeight(0f), EPS);
    }
}
