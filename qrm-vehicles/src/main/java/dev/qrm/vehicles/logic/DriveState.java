package dev.qrm.vehicles.logic;

/**
 * Lo stato dinamico del veicolo: direzione del muso in gradi (come l'imbardata di Minecraft, 0 = verso +Z),
 * angolo delle ruote anteriori e velocità nel mondo in blocchi per tick.
 */
public record DriveState(double heading, double steerAngle, double vx, double vz) {

    public static DriveState rest(double heading) {
        return new DriveState(heading, 0, 0, 0);
    }

    public double speed() {
        return Math.sqrt(vx * vx + vz * vz);
    }

    /** Componente della velocità lungo il muso: negativa in retromarcia. */
    public double forwardSpeed() {
        double h = Math.toRadians(heading);
        return vx * -Math.sin(h) + vz * Math.cos(h);
    }
}
