package dev.qrm.client.api.rig;

/**
 * Trasformazione di un osso: posizione in unità Blockbench (16 = 1 blocco), rotazione in gradi, scala moltiplicativa.
 * Gli array sono copiati in ingresso e in uscita: {@link #REST} non si può alterare dall'esterno.
 */
public record BoneTransform(float[] position, float[] rotationDeg, float[] scale) {

    /** Riposo: posizione 0, rotazione 0, scala 1. */
    public static final BoneTransform REST =
            new BoneTransform(new float[] {0, 0, 0}, new float[] {0, 0, 0}, new float[] {1, 1, 1});

    public BoneTransform {
        position = check(position);
        rotationDeg = check(rotationDeg);
        scale = check(scale);
    }

    private static float[] check(float[] a) {
        if (a == null || a.length != 3) throw new IllegalArgumentException("Servono tre componenti");
        return a.clone();
    }

    @Override
    public float[] position() {
        return position.clone();
    }

    @Override
    public float[] rotationDeg() {
        return rotationDeg.clone();
    }

    @Override
    public float[] scale() {
        return scale.clone();
    }
}
