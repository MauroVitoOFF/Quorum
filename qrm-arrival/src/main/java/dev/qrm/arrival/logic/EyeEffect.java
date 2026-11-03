package dev.qrm.arrival.logic;

/** L'effetto a schermo dell'apertura degli occhi: valori da 1 (tutto buio, occhi chiusi) a 0 (visuale libera). */
public final class EyeEffect {
    private EyeEffect() {}

    /** Il velo scuro su tutto lo schermo, che si schiarisce per l'intera durata del risveglio. */
    public static float veil(int elapsed, int totalTicks) {
        float rest = 1.0f - progress(elapsed, totalTicks);
        return rest * rest;
    }

    /** Le palpebre: quanto sono chiuse mentre il personaggio è a terra. A fine fase LYING sono aperte. */
    public static float eyelids(int elapsed, int lyingTicks) {
        float rest = 1.0f - progress(elapsed, lyingTicks);
        return rest * rest;
    }

    private static float progress(int elapsed, int total) {
        if (total <= 0) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) elapsed / total));
    }
}
