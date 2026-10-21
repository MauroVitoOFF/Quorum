package dev.qrm.police.weapon.logic.anim;

/**
 * Keyframe di un canale: istante in secondi, valore a tre componenti e interpolazione verso il keyframe successivo.
 */
public record AnimKey(double t, float[] v, Interp interp) {

    /** Modalità di interpolazione del segmento che parte da questo keyframe. */
    public enum Interp {
        LINEAR, STEP, CATMULLROM;

        /** Interpreta la stringa del file; valori sconosciuti sono un errore esplicito. */
        public static Interp parse(String s) {
            if (s == null) return LINEAR;
            return switch (s) {
                case "linear" -> LINEAR;
                case "step" -> STEP;
                case "catmullrom" -> CATMULLROM;
                default -> throw new IllegalArgumentException("Interpolazione sconosciuta: " + s);
            };
        }
    }

    public AnimKey {
        if (v == null || v.length != 3) throw new IllegalArgumentException("Il valore di un keyframe ha tre componenti");
        v = v.clone();
        if (interp == null) interp = Interp.LINEAR;
    }

    @Override
    public float[] v() {
        return v.clone();
    }
}
