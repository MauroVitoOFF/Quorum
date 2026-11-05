package dev.qrm.vehicles.logic;

/**
 * Lo stato dinamico del veicolo: direzione del muso in gradi (come l'imbardata di Minecraft, 0 = verso +Z),
 * angolo delle ruote anteriori, velocità nel mondo in blocchi per tick, marcia (1..n avanti, -1 retromarcia) e regime
 * del motore in giri/min (0 a motore spento) e velocità di imbardata in rad/s (positiva a destra).
 */
public record DriveState(double heading, double steerAngle, double vx, double vz, int gear, double rpm,
        double yawRate) {

    public DriveState(double heading, double steerAngle, double vx, double vz) {
        this(heading, steerAngle, vx, vz, 1, 0, 0);
    }

    public static DriveState rest(double heading) {
        return new DriveState(heading, 0, 0, 0);
    }

    /** Stesso stato (marcia e regime compresi) con muso e velocità cambiati, per esempio dopo una collisione. */
    public DriveState withMotion(double newHeading, double newVx, double newVz) {
        return new DriveState(newHeading, steerAngle, newVx, newVz, gear, rpm, yawRate);
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
