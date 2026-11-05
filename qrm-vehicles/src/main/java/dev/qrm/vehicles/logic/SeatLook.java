package dev.qrm.vehicles.logic;

/** Come si muove la testa di chi sta a bordo: il corpo resta fermo col veicolo, la testa segue lo sguardo. */
public final class SeatLook {

    /** Quanto la testa può girare rispetto al corpo, in gradi per lato (oltre il collo "si spezzerebbe"). */
    public static final float MAX_HEAD_TURN = 90f;

    private SeatLook() {}

    /** Gradi in (-180, 180]. */
    public static float wrap(float deg) {
        float d = deg % 360f;
        if (d > 180f) d -= 360f;
        if (d <= -180f) d += 360f;
        return d;
    }

    /** Rotazione della testa rispetto al corpo (che sta a {@code bodyYaw}), limitata a ±{@link #MAX_HEAD_TURN}. */
    public static float headRelativeYaw(float lookYaw, float bodyYaw) {
        float rel = wrap(lookYaw - bodyYaw);
        return Math.max(-MAX_HEAD_TURN, Math.min(MAX_HEAD_TURN, rel));
    }
}
