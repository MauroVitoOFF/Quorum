package dev.qrm.police.weapon.logic.anim;

import dev.qrm.client.api.rig.BoneTransform;

import java.util.HashMap;
import java.util.Map;

/** Valutatore puro delle clip: campiona e miscela le trasformazioni degli ossi. */
public final class AnimEvaluator {

    private static final float[] REST_POS = {0, 0, 0};
    private static final float[] REST_SCALE = {1, 1, 1};

    private AnimEvaluator() {}

    /**
     * Campiona la clip all'istante {@code t} (secondi). Restituisce solo gli ossi con almeno un canale; gli assenti
     * valgono riposo per chi chiama. Non applica il ciclo: per le clip in loop è chi chiama a ridurre {@code t}.
     */
    public static Map<String, BoneTransform> sample(AnimClip clip, double t) {
        Map<String, BoneTransform> out = new HashMap<>();
        for (Map.Entry<String, AnimClip.Bone> e : clip.bones().entrySet()) {
            AnimClip.Bone b = e.getValue();
            if (b.position() == null && b.rotation() == null && b.scale() == null) continue;
            out.put(e.getKey(), new BoneTransform(
                    value(b.position(), t, REST_POS),
                    value(b.rotation(), t, REST_POS),
                    value(b.scale(), t, REST_SCALE)));
        }
        return out;
    }

    private static float[] value(AnimChannel ch, double t, float[] rest) {
        return ch == null ? rest.clone() : ch.sample(t, rest);
    }

    /** Miscela lineare {@code a*(1-w) + b*w}; un osso assente da una delle due mappe vale riposo. */
    public static Map<String, BoneTransform> blend(Map<String, BoneTransform> a, Map<String, BoneTransform> b, float w) {
        Map<String, BoneTransform> out = new HashMap<>();
        for (String k : a.keySet()) out.put(k, mix(a.get(k), b.getOrDefault(k, BoneTransform.REST), w));
        for (String k : b.keySet()) {
            if (!a.containsKey(k)) out.put(k, mix(BoneTransform.REST, b.get(k), w));
        }
        return out;
    }

    private static BoneTransform mix(BoneTransform x, BoneTransform y, float w) {
        return new BoneTransform(lerp(x.position(), y.position(), w), lerp(x.rotationDeg(), y.rotationDeg(), w),
                lerp(x.scale(), y.scale(), w));
    }

    private static float[] lerp(float[] x, float[] y, float w) {
        float[] o = new float[3];
        for (int i = 0; i < 3; i++) o[i] = x[i] + (y[i] - x[i]) * w;
        return o;
    }
}
