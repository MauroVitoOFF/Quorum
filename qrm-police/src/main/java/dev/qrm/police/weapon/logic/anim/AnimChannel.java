package dev.qrm.police.weapon.logic.anim;

import java.util.List;

/**
 * Canale di animazione (posizione, rotazione o scala di un osso): keyframe ordinati per tempo.
 * Semantica di Blockbench: prima del primo keyframe vale il riposo, dopo l'ultimo l'ultimo valore.
 */
public record AnimChannel(List<AnimKey> keys) {

    public AnimChannel {
        keys = List.copyOf(keys);
    }

    /** Valore del canale all'istante {@code t}; {@code rest} se il canale è vuoto o t precede il primo keyframe. */
    public float[] sample(double t, float[] rest) {
        int n = keys.size();
        if (n == 0 || t < keys.get(0).t()) return rest.clone();
        AnimKey last = keys.get(n - 1);
        if (t >= last.t()) return last.v();
        // Segmento [a, b] con a.t <= t < b.t.
        int i = 0;
        while (keys.get(i + 1).t() <= t) i++;
        AnimKey a = keys.get(i);
        AnimKey b = keys.get(i + 1);
        if (a.interp() == AnimKey.Interp.STEP) return a.v();
        double span = b.t() - a.t();
        float f = span <= 0 ? 1f : (float) ((t - a.t()) / span);
        float[] pa = a.v();
        float[] pb = b.v();
        float[] out = new float[3];
        if (a.interp() == AnimKey.Interp.CATMULLROM || b.interp() == AnimKey.Interp.CATMULLROM) {
            float[] p0 = (i > 0 ? keys.get(i - 1) : a).v();
            float[] p3 = (i + 2 < n ? keys.get(i + 2) : b).v();
            float f2 = f * f;
            float f3 = f2 * f;
            for (int c = 0; c < 3; c++) {
                out[c] = 0.5f * (2f * pa[c]
                        + (-p0[c] + pb[c]) * f
                        + (2f * p0[c] - 5f * pa[c] + 4f * pb[c] - p3[c]) * f2
                        + (-p0[c] + 3f * pa[c] - 3f * pb[c] + p3[c]) * f3);
            }
        } else {
            for (int c = 0; c < 3; c++) out[c] = pa[c] + (pb[c] - pa[c]) * f;
        }
        return out;
    }
}
