package dev.qrm.vehicles.logic;

/**
 * Il modello di guida, un tick alla volta e senza dipendere da Minecraft. Morbido ma prevedibile: l'accelerazione
 * è graduale, il muso gira in base a velocità e passo, e la velocità laterale decade da sola (una piccola derapata
 * che si raddrizza).
 */
public final class DrivingModel {

    private DrivingModel() {}

    public static DriveState step(VehicleDef def, DriveState s, DriveInput rawInput, Surface surface,
            boolean engineOn) {
        DriveInput in = rawInput.clamped();
        double top = def.maxSpeed() * surface.speedFactor();
        double reverseTop = def.reverseMaxSpeed() * surface.speedFactor();

        // Sterzo: le ruote inseguono l'angolo richiesto con una velocità limitata.
        // Più si va veloci, meno le ruote si possono girare: a regime restano il 40% dell'angolo massimo.
        double speedRatio = Math.min(1, Math.abs(s.forwardSpeed()) / def.maxSpeed());
        double target = in.steer() * def.maxSteerDeg() * (1 - 0.6 * speedRatio);
        double steer = s.steerAngle() + Math.max(-def.steerRateDeg(), Math.min(def.steerRateDeg(), target - s.steerAngle()));

        // Il muso gira in proporzione alla velocità in avanti e al passo (modello a bicicletta); la velocità nel
        // mondo non cambia in questo passaggio, quindi parte della spinta finisce di lato (derapata).
        double forward = s.forwardSpeed();
        double heading = s.heading() + Math.toDegrees(forward / def.wheelBase() * Math.tan(Math.toRadians(steer)));
        double h = Math.toRadians(heading);
        double vF = s.vx() * -Math.sin(h) + s.vz() * Math.cos(h);
        double vL = s.vx() * Math.cos(h) + s.vz() * Math.sin(h);

        // Longitudinale.
        if (engineOn && in.throttle() > 0) {
            double cap = top * in.throttle();
            if (vF < cap) vF = Math.min(vF + def.accel() * in.throttle(), cap);
        } else if (engineOn && in.throttle() < 0) {
            if (vF > 0) {
                vF = Math.max(0, vF - def.brake() * -in.throttle());
            } else {
                double cap = reverseTop * -in.throttle();
                vF = Math.max(vF + def.accel() * in.throttle(), -cap);
            }
        }
        vF -= Math.signum(vF) * Math.min(Math.abs(vF), def.drag());
        if (in.brake()) vF -= Math.signum(vF) * Math.min(Math.abs(vF), def.brake());
        // Entrando in un fondo più lento di quello che si teneva, la velocità rientra gradualmente.
        double limit = vF >= 0 ? top : reverseTop;
        if (Math.abs(vF) > limit) vF -= Math.signum(vF) * Math.min(Math.abs(vF) - limit, def.accel());

        // La velocità laterale decade: meno aderenza, derapata più lunga.
        vL *= 1 - Math.min(1, def.grip() * surface.gripFactor());

        // Di nuovo nel mondo.
        double vx = vF * -Math.sin(h) + vL * Math.cos(h);
        double vz = vF * Math.cos(h) + vL * Math.sin(h);
        return new DriveState(heading, steer, vx, vz);
    }
}
