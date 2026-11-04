package dev.qrm.police.weapon.logic.mesh;

import dev.qrm.client.api.rig.BoneTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Posa dei bracci del giocatore in prima persona, calcolata a ogni fotogramma (logica pura, solo JOML).
 *
 * <p>Il braccio è rigido, senza gomito: il pugno sta su un punto di una parte dell'arma e il braccio punta da una
 * spalla fissa, dietro e di lato alla camera. Da questi due punti si ricava la cornice del braccio (origine nella
 * spalla, asse Y dal pugno verso la spalla); la mano destra sta sempre su un punto di presa, la sinistra segue un
 * programma di passaggi tra punti di presa (impugnatura, caricatore, carrello…).
 *
 * <p>Unità: i punti dei bersagli sono in unità del modello (16 = 1 blocco), come nel rig; le matrici dei bone e i
 * risultati sono in blocchi.
 */
public final class ArmSolver {
    /** Il lato esterno del braccio guarda verso destra. */
    private static final Vector3f OUTSIDE = new Vector3f(1f, 0f, 0f);
    /** Il pugno nel sistema del braccio rispetto al perno (spalla): asse del braccio a x = ±1, pugno a y = −8. */
    private static final Vector3f FIST_RIGHT = new Vector3f(1f, -8f, 0f).div(16f);
    private static final Vector3f FIST_LEFT = new Vector3f(-1f, -8f, 0f).div(16f);
    private static final float UNIT = 1f / 16f;
    /** Nome speciale di un passaggio del programma: la mano non si sposta da dove si trova. */
    public static final String FREEZE = "freeze";

    private ArmSolver() {}

    /** Punto di presa: il bone a cui è legato, il punto (unità del modello) e la spalla da cui arriva il braccio. */
    public record Target(String bone, float[] point, float[] shoulder) {}

    /** Passaggio del programma della mano sinistra: tra {@code start} e {@code end} (secondi) va verso {@code target}. */
    public record Step(double start, double end, String target) {}

    /** Dove sta il pugno e da dove arriva il braccio, in blocchi e nello spazio del modello. */
    public record Hand(Vector3f fist, Vector3f shoulder) {}

    /**
     * La posa senza le scale: un bone nascosto (scala 0, per esempio il caricatore che sparisce mentre si cambia)
     * non deve trascinare la mano sul suo perno. Le matrici per le mani si compongono da questa posa.
     */
    public static Map<String, BoneTransform> withoutScale(Map<String, BoneTransform> pose) {
        Map<String, BoneTransform> out = new HashMap<>();
        for (Map.Entry<String, BoneTransform> e : pose.entrySet()) {
            BoneTransform t = e.getValue();
            out.put(e.getKey(), new BoneTransform(t.position(), t.rotationDeg(), new float[] {1f, 1f, 1f}));
        }
        return out;
    }

    /** Mano su un punto di presa, con le matrici dei bone dell'istante (blocchi). */
    public static Hand at(Target t, Map<String, Matrix4f> mats) {
        Matrix4f m = mats.get(t.bone());
        Vector3f p = new Vector3f(t.point()[0], t.point()[1], t.point()[2]).mul(UNIT);
        if (m != null) m.transformPosition(p);
        return new Hand(p, new Vector3f(t.shoulder()[0], t.shoulder()[1], t.shoulder()[2]).mul(UNIT));
    }

    /**
     * Mano al tempo {@code t} dell'azione (a ogni mano il suo programma): parte dal punto {@code defaultTarget}, e per ogni passaggio già
     * iniziato scorre dal punto in cui si trova verso il suo, con una curva dolce. Pugno e spalla scorrono insieme.
     * {@link #FREEZE} fissa il punto di partenza del passaggio successivo.
     */
    public static Hand follow(Map<String, Target> targets, String defaultTarget, List<Step> schedule, double t,
            Map<String, Matrix4f> mats) {
        Hand base = at(targets.get(defaultTarget), mats);
        Vector3f fist = new Vector3f(base.fist());
        Vector3f shoulder = new Vector3f(base.shoulder());
        Vector3f frozen = null;
        for (Step s : schedule) {
            if (t < s.start()) break;
            if (FREEZE.equals(s.target())) {
                frozen = new Vector3f(fist);
                continue;
            }
            Hand dest = at(targets.get(s.target()), mats);
            float w = s.end() <= s.start() ? 1f : smooth((t - s.start()) / (s.end() - s.start()));
            Vector3f src = frozen != null ? frozen : new Vector3f(fist);
            fist = new Vector3f(src).lerp(dest.fist(), w);
            shoulder = new Vector3f(shoulder).lerp(dest.shoulder(), w);
            if (w >= 1f) frozen = null;
        }
        return new Hand(fist, shoulder);
    }

    /**
     * Cornice del braccio (blocchi, spazio del modello): origine nel perno, che è la spalla del modello del giocatore,
     * con il pugno su {@code hand.fist()} e il braccio che punta da {@code hand.shoulder()}.
     */
    public static Matrix4f frame(boolean right, Hand hand) {
        Vector3f d = new Vector3f(hand.fist()).sub(hand.shoulder()).normalize();
        Vector3f y = new Vector3f(d).negate(); // +Y del braccio va dal pugno verso la spalla
        Vector3f x = new Vector3f(OUTSIDE).sub(new Vector3f(y).mul(OUTSIDE.dot(y))).normalize();
        Vector3f z = new Vector3f(x).cross(y);
        Vector3f fistLocal = right ? FIST_RIGHT : FIST_LEFT;
        // origine = pugno − R · pugno locale
        Vector3f origin = new Vector3f(hand.fist())
                .sub(new Vector3f(x).mul(fistLocal.x).add(new Vector3f(y).mul(fistLocal.y))
                        .add(new Vector3f(z).mul(fistLocal.z)));
        return new Matrix4f(x.x, x.y, x.z, 0f, y.x, y.y, y.z, 0f, z.x, z.y, z.z, 0f, origin.x, origin.y, origin.z, 1f);
    }

    private static float smooth(double w) {
        double c = Math.max(0.0, Math.min(1.0, w));
        return (float) (c * c * (3 - 2 * c));
    }
}
