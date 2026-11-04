package dev.qrm.client.api.body;

/**
 * Colore di una zona in base alla gravità: grigio scuro se illesa, poi giallo, arancione, rosso e, a gravità piena,
 * un bruno scuro (zona fuori uso). La texture è in luminosità neutra e si moltiplica per questo colore.
 */
public final class BodyTint {
    /** Illesa. */
    public static final int HEALTHY = 0xFF5C606A;
    private static final float[] STOPS = {0f, 0.25f, 0.5f, 0.75f, 1f};
    private static final int[] COLORS = {HEALTHY, 0xFFFFD746, 0xFFFA8C32, 0xFFEB3C37, 0xFF5F4650};

    private BodyTint() {}

    /** Colore ARGB opaco per una gravità; i valori fuori da 0..1 si limitano agli estremi. */
    public static int color(float severity) {
        if (!(severity > 0f)) return COLORS[0];
        if (severity >= 1f) return COLORS[COLORS.length - 1];
        int i = 1;
        while (severity > STOPS[i]) i++;
        float t = (severity - STOPS[i - 1]) / (STOPS[i] - STOPS[i - 1]);
        return lerp(COLORS[i - 1], COLORS[i], t);
    }

    private static int lerp(int a, int b, float t) {
        int out = 0xFF000000;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ca = (a >> shift) & 0xFF;
            int cb = (b >> shift) & 0xFF;
            out |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return out;
    }
}
