package dev.qrm.vehicles.logic;

/**
 * Dal riferimento del modello (X verso la destra del veicolo, davanti = -Z) a uno scostamento nel mondo, per un
 * veicolo con una certa imbardata (come in Minecraft: 0 guarda verso +Z, 90 verso -X).
 */
public final class SeatMath {

    private SeatMath() {}

    /** Scostamento orizzontale {x, z} nel mondo di un punto del modello a ({@code mx}, {@code mz}), in blocchi. */
    public static double[] toWorld(double mx, double mz, double yawDeg) {
        double y = Math.toRadians(yawDeg);
        double cos = Math.cos(y);
        double sin = Math.sin(y);
        return new double[] {-cos * mx + sin * mz, -sin * mx - cos * mz};
    }
}
