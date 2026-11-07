package dev.qrm.vehicles.logic;

/**
 * Lo sterzo del guidatore è un tasto: -1, 0 o +1. Dato così, le ruote scattano da un lato all'altro. Questa è la mano
 * che gira il volante: l'input "morbido" sale piano verso il tasto premuto (più piano quanto più si va veloci) e torna
 * al centro più in fretta quando lo si lascia o si cambia verso.
 */
public final class SteerSmoothing {
    private SteerSmoothing() {}

    /** Quanto si sposta l'input per tick verso il tasto premuto da fermi e a velocità massima (tutta la corsa è 1). */
    public static final double RISE_SLOW = 0.12;
    public static final double RISE_FAST = 0.08;
    /** Ritorno al centro (o inversione) per tick. */
    public static final double RETURN = 0.2;

    /** Il prossimo valore dell'input di sterzo, in -1..1. {@code speedRatio} è la velocità sulla massima, 0..1. */
    public static double step(double current, double target, double speedRatio) {
        double ratio = Math.max(0, Math.min(1, speedRatio));
        boolean returning = target == 0 || (current != 0 && Math.signum(target) != Math.signum(current));
        double rate = returning ? RETURN : RISE_SLOW + (RISE_FAST - RISE_SLOW) * ratio;
        double next = current + Math.max(-rate, Math.min(rate, target - current));
        return Math.max(-1, Math.min(1, next));
    }
}
