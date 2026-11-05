package dev.qrm.vehicles.logic;

import dev.qrm.client.api.rig.BoneTransform;
import java.util.Map;

/**
 * La posa delle ossa della Jeep a partire da ruote, sterzo e assetto. Gli altri ossa restano a riposo (non sono
 * nella mappa). Angoli in gradi nel riferimento del modello: davanti = -Z, destra = +X.
 */
public final class VehiclePose {

    private VehiclePose() {}

    /**
     * @param wheelDeg rotazione delle ruote, positiva quando il veicolo va avanti
     * @param steerDeg angolo di sterzo, positivo a destra
     * @param pitchDeg beccheggio della cassa, positivo muso su
     * @param rollDeg rollio della cassa, positivo lato destro su
     */
    public static Map<String, BoneTransform> pose(float wheelDeg, float steerDeg, float pitchDeg, float rollDeg) {
        // Girare a destra porta il davanti (-Z) verso +X: rotazione negativa attorno a Y. Rotolare in avanti porta
        // la cima della ruota verso -Z: rotazione negativa attorno a X.
        BoneTransform steer = rotation(0, -steerDeg, 0);
        BoneTransform wheel = rotation(-wheelDeg, 0, 0);
        return Map.of(
                "body", rotation(pitchDeg, 0, rollDeg),
                "steer_fl", steer,
                "steer_fr", steer,
                "wheel_fl", wheel,
                "wheel_fr", wheel,
                "wheel_rl", wheel,
                "wheel_rr", wheel);
    }

    private static BoneTransform rotation(float x, float y, float z) {
        return new BoneTransform(new float[] {0, 0, 0}, new float[] {x, y, z}, new float[] {1, 1, 1});
    }
}
