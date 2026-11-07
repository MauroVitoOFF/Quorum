package dev.qrm.vehicles.logic;

/**
 * Guida "classica", pensata per un feeling molto diretto: il muso gira per geometria con uno sterzo ampio e lento da
 * girare, e le gomme riportano la direzione del moto verso il muso al massimo di una certa quantità per tick.
 * Finché lo scarto è dentro quel limite l'auto va dove punta il muso (nessuna deriva); oltre scivola.
 *
 * <p>È una descrizione a parole di come ragiona un riferimento che ci piace, riscritta da zero: non c'è codice di
 * terzi qui. Tutte le quantità angolari sono in gradi per tick (20 tick al secondo).
 */
public final class ClassicHandling {
    private ClassicHandling() {}

    private static final double TPS = 20;
    /** Metri al secondo per ogni unità di velocità del modello di riferimento (0,35 blocchi/tick × 20 tick). */
    private static final double MS_PER_UNIT = 7.0;

    /** Angolo massimo delle ruote anteriori, in gradi. */
    public static final double LOCK_DEG = 45;
    /** Gradi di sterzo per tick col tasto premuto e senza (la mano torna al centro il doppio più in fretta). */
    public static final double TURN_RATE = 2.0;
    public static final double RETURN_RATE = 4.0;
    /** Sopra questa velocità (unità del riferimento) lo sterzo comincia a perdere efficacia, in modo esponenziale. */
    private static final double FADE_START = 0.35;
    /** Quanto presto lo sterzo perde efficacia con la velocità: più è alto, più tiene (0..1). */
    public static final double STEER_KEEP = 0.4;
    /** Quanto il muso si gira in più verso l'esterno quando l'auto scivola (gradi per tick a scivolamento pieno). */
    public static final double OVERSTEER = 0.6;
    /** Quanti gradi per tick le gomme possono raddrizzare la direzione del moto: somma dei quattro pneumatici. */
    public static final double GRIP_DEG_PER_TICK = 3.4;
    /** Il muso gira al massimo questa volta la presa delle gomme (per tick): oltre l'auto si avviterebbe. */
    public static final double YAW_OVER_GRIP = 1.1;
    /** Col freno a mano il posteriore tiene poco e il muso gira di più, per far partire la derapata. */
    private static final double HANDBRAKE_GRIP = 0.3;
    private static final double HANDBRAKE_YAW = 1.3;

    /** L'angolo delle ruote al prossimo tick: sale piano verso il tasto premuto, torna al centro più in fretta. */
    public static double steerAngle(double current, double input) {
        double target = Math.max(-1, Math.min(1, input)) * LOCK_DEG;
        // Verso un lato più a fondo (o dal centro verso un lato): piano. Verso il centro o l'altro lato: in fretta.
        boolean widening = target != 0 && (current == 0 || Math.signum(target) == Math.signum(current))
                && Math.abs(target) > Math.abs(current);
        double rate = widening ? TURN_RATE : RETURN_RATE;
        double next = current + Math.max(-rate, Math.min(rate, target - current));
        return Math.max(-LOCK_DEG, Math.min(LOCK_DEG, next));
    }

    /** Velocità di imbardata per un angolo delle ruote e una velocità, in gradi per tick (positiva a destra). */
    public static double yawDegPerTick(double steerDeg, double speedMs, double wheelBase, boolean handbrake) {
        double v = speedMs / MS_PER_UNIT;
        double fade = Math.abs(v) > FADE_START ? Math.pow(0.3, Math.abs(v) * (1 - STEER_KEEP) - FADE_START) : 1;
        return steerDeg / wheelBase * v * fade * 0.5 * (handbrake ? HANDBRAKE_YAW : 1);
    }

    /**
     * Un tick di dinamica laterale.
     *
     * @param u0 velocità lungo il muso (m/s, negativa in retromarcia)
     * @param vy0 velocità laterale (m/s, positiva a sinistra), nel riferimento del muso all'inizio del tick
     * @param gripScale aderenza del fondo (1 asfalto, meno su ghiaccio o terra)
     * @return {u, vy, r}: velocità nel muso girato, e imbardata in rad/s (positiva a destra)
     */
    public static double[] lateral(double u0, double vy0, double steerDeg, double wheelBase, double gripScale,
            boolean handbrake) {
        // 1. Il muso gira per sterzo; la velocità del mondo resta quella, riespressa nel muso girato.
        double yaw1 = yawDegPerTick(steerDeg, u0, wheelBase, handbrake);
        // Il muso non gira molto più in fretta di quanto le gomme possano seguirlo: si scivola, ma non si avvita.
        double yawCap = YAW_OVER_GRIP * GRIP_DEG_PER_TICK * Math.max(0.05, gripScale) * (handbrake ? HANDBRAKE_YAW : 1);
        yaw1 = Math.max(-yawCap, Math.min(yawCap, yaw1));
        double[] v = rotate(u0, vy0, Math.toRadians(yaw1));
        double u = v[0];
        double w = v[1];

        // 2. Le gomme riportano il moto verso il muso (o verso la coda, in retromarcia) fino al loro limite.
        double speed = Math.hypot(u, w);
        double yaw2 = 0;
        if (speed > 0.05) {
            double dir = u >= 0 ? 1 : -1;
            double slipDeg = Math.toDegrees(Math.atan2(Math.abs(w), Math.abs(u)));
            double grip = GRIP_DEG_PER_TICK * Math.max(0.05, gripScale) * (handbrake ? HANDBRAKE_GRIP : 1);
            double factor = slipDeg > grip ? grip / slipDeg : 1;
            // 3. Con lo scarto, il muso gira un poco in più nel verso dello scivolamento (sovrasterzo).
            if (dir > 0) yaw2 = OVERSTEER * Math.sin(Math.toRadians(slipDeg)) * Math.signum(w);
            double idealU = dir * speed;
            u = idealU * factor + u * (1 - factor);
            w = w * (1 - factor);
        }
        double[] out = rotate(u, w, Math.toRadians(yaw2));
        return new double[] {out[0], out[1], Math.toRadians(yaw1 + yaw2) * TPS};
    }

    /** La stessa velocità del mondo, espressa in un muso girato (a destra) di {@code d} radianti. */
    private static double[] rotate(double u, double w, double d) {
        return new double[] {u * Math.cos(d) - w * Math.sin(d), w * Math.cos(d) + u * Math.sin(d)};
    }
}
