package dev.qrm.vehicles.logic;

/**
 * Quando e quanto emettere particelle dalle ruote e dallo scarico. Solo regole, senza Minecraft: il client le applica
 * a ogni tick. Velocità in m/s, accelerazioni in m/s², quantità in particelle per ruota e per tick (frazioni = probabilità).
 */
public final class ParticleRules {

    /** Sotto questa velocità le ruote non sollevano niente. */
    public static final double MIN_DUST_SPEED = 2.0;
    /** Velocità laterale (m/s) oltre la quale le gomme strisciano. */
    public static final double SKID_LATERAL = 3.0;
    /** Frenata (m/s²) oltre la quale le gomme fischiano, con almeno MIN_SKID_SPEED. */
    public static final double SKID_DECEL = 9.0;
    public static final double MIN_SKID_SPEED = 4.0;

    private ParticleRules() {}

    /** Particelle di polvere, sabbia o neve per ruota e per tick: dipendono dal fondo e crescono con la velocità. */
    public static double dust(String surfaceId, double speedMs) {
        double base = switch (surfaceId) {
            case "dirt" -> 0.25;
            case "sand" -> 0.6;
            case "snow" -> 0.5;
            default -> 0; // asfalto e ghiaccio non sollevano nulla
        };
        double speed = Math.abs(speedMs);
        if (speed < MIN_DUST_SPEED) return 0;
        return base * Math.min(speed, 25) / 12.0;
    }

    /** Le gomme strisciano: di traverso, o frenando forte. */
    public static boolean skidding(double lateralMs, double decelMs2, double speedMs) {
        double speed = Math.abs(speedMs);
        if (Math.abs(lateralMs) > SKID_LATERAL) return true;
        return speed > MIN_SKID_SPEED && decelMs2 > SKID_DECEL;
    }

    /** Una marcia avanti che cambia: sbuffo allo scarico. */
    public static boolean shifted(int previousGear, int gear) {
        return previousGear > 0 && gear > 0 && previousGear != gear;
    }
}
