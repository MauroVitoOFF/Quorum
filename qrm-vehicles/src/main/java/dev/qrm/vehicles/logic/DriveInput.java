package dev.qrm.vehicles.logic;

/** Cosa chiede il guidatore in un tick: gas/retromarcia e sterzo in [-1, 1] (sterzo positivo = a destra), freno. */
public record DriveInput(double throttle, double steer, boolean brake) {

    public static final DriveInput NONE = new DriveInput(0, 0, false);

    /** Porta i valori nell'intervallo ammesso; un valore non numerico vale 0. Va applicato a ogni input di rete. */
    public DriveInput clamped() {
        return new DriveInput(clamp(throttle), clamp(steer), brake);
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) return 0;
        return Math.max(-1, Math.min(1, v));
    }
}
