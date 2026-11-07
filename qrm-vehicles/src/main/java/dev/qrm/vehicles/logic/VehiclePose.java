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
        return pose(wheelDeg, steerDeg, pitchDeg, rollDeg, 0);
    }

    /** Gradi di volante per ogni grado di sterzo delle ruote. */
    public static final float STEERING_WHEEL_RATIO = 5f;

    /**
     * Come sopra, con lo spostamento verticale della cassa sulle sospensioni ({@code heavePx}, unità del modello)
     * e il volante che gira con lo sterzo.
     */
    public static Map<String, BoneTransform> pose(float wheelDeg, float steerDeg, float pitchDeg, float rollDeg,
            float heavePx) {
        return pose(wheelDeg, steerDeg, pitchDeg, rollDeg, heavePx, Map.of());
    }

    /** Come sopra, con in più la posa delle parti mobili ({@link VehicleParts#pose}); le ossa base prevalgono. */
    public static Map<String, BoneTransform> pose(float wheelDeg, float steerDeg, float pitchDeg, float rollDeg,
            float heavePx, Map<String, BoneTransform> extra) {
        Map<String, BoneTransform> out = new java.util.HashMap<>(extra);
        out.putAll(base(wheelDeg, steerDeg, pitchDeg, rollDeg, heavePx));
        return out;
    }

    private static Map<String, BoneTransform> base(float wheelDeg, float steerDeg, float pitchDeg, float rollDeg,
            float heavePx) {
        // Girare a destra porta il davanti (-Z) verso +X: rotazione negativa attorno a Y. Rotolare in avanti porta
        // la cima della ruota verso -Z: rotazione negativa attorno a X.
        BoneTransform steer = rotation(0, -steerDeg, 0);
        BoneTransform wheel = rotation(-wheelDeg, 0, 0);
        // Il volante gira attorno al suo asse (Z): a destra in senso orario visto dal guidatore.
        return Map.of(
                "body", new BoneTransform(new float[] {0, heavePx, 0}, new float[] {pitchDeg, 0, rollDeg},
                        new float[] {1, 1, 1}),
                "steering_wheel", steeringWheel(-steerDeg * STEERING_WHEEL_RATIO),
                "steer_fl", steer,
                "steer_fr", steer,
                "wheel_fl", wheel,
                "wheel_fr", wheel,
                "wheel_rl", wheel,
                "wheel_rr", wheel);
    }

    /** Inclinazione della piantana dello sterzo, cotta nei vertici (osso steering_tilt, in gradi attorno a X). */
    private static final double COLUMN_TILT_DEG = -20;
    /** Dal perno dell'osso steering_wheel al centro vero del volante, dopo l'inclinazione (unità del modello). */
    private static final double[] HUB_OFFSET = {0, 3.6 * Math.sin(Math.toRadians(-COLUMN_TILT_DEG)),
        3.6 * Math.cos(Math.toRadians(COLUMN_TILT_DEG)) - 3.6};

    /**
     * Il volante gira attorno all'asse inclinato della piantana, passando dal suo centro: Rx(t)·Rz(θ)·Rx(-t) scritta
     * come rotazione Z·Y·X, con la traslazione che sposta il centro di rotazione dal perno dell'osso al mozzo.
     */
    static BoneTransform steeringWheel(float angleDeg) {
        double t = Math.toRadians(COLUMN_TILT_DEG);
        double th = Math.toRadians(angleDeg);
        double[][] rx = {{1, 0, 0}, {0, Math.cos(t), -Math.sin(t)}, {0, Math.sin(t), Math.cos(t)}};
        double[][] rxInv = {{1, 0, 0}, {0, Math.cos(t), Math.sin(t)}, {0, -Math.sin(t), Math.cos(t)}};
        double[][] rz = {{Math.cos(th), -Math.sin(th), 0}, {Math.sin(th), Math.cos(th), 0}, {0, 0, 1}};
        double[][] m = mul(mul(rx, rz), rxInv);
        double y = -Math.asin(Math.max(-1, Math.min(1, m[2][0])));
        double x = Math.atan2(m[2][1], m[2][2]);
        double z = Math.atan2(m[1][0], m[0][0]);
        double[] d = HUB_OFFSET;
        float[] pos = new float[3];
        for (int i = 0; i < 3; i++) {
            pos[i] = (float) (d[i] - (m[i][0] * d[0] + m[i][1] * d[1] + m[i][2] * d[2]));
        }
        return new BoneTransform(pos,
                new float[] {(float) Math.toDegrees(x), (float) Math.toDegrees(y), (float) Math.toDegrees(z)},
                new float[] {1, 1, 1});
    }

    private static double[][] mul(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j];
            }
        }
        return r;
    }

    private static BoneTransform rotation(float x, float y, float z) {
        return new BoneTransform(new float[] {0, 0, 0}, new float[] {x, y, z}, new float[] {1, 1, 1});
    }
}
