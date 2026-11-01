package dev.qrm.police.weapon.logic;

import java.util.random.RandomGenerator;

/** Costanti e calcoli di balistica della pistola di servizio (logica pura). */
public final class Ballistics {
    /** Gittata massima in blocchi. */
    public static final double RANGE = def().range();

    /** Accovacciati la dispersione si riduce di questo fattore (0,5 = dimezzata). */
    public static final double CROUCH_FACTOR = def().crouchFactor();

    private Ballistics() {}

    /** I valori vengono da {@code weapons/police_pistol.json}. */
    private static WeaponDef.Ballistics def() {
        return WeaponDef.pistol().ballistics();
    }

    public static float damage(boolean headshot) {
        return headshot ? def().headDamage() : def().bodyDamage();
    }

    /** Semiapertura del cono di dispersione, in gradi. */
    public static double spreadDegrees(boolean aiming) {
        return spreadDegrees(aiming, false);
    }

    /** Come {@link #spreadDegrees(boolean)}, dimezzata se il tiratore è accovacciato. */
    public static double spreadDegrees(boolean aiming, boolean crouching) {
        double base = aiming ? def().aimSpread() : def().hipSpread();
        return crouching ? base * CROUCH_FACTOR : base;
    }

    /**
     * Devia la direzione (gradi, convenzione Minecraft) entro il cono di dispersione, con
     * campionamento uniforme sul disco angolare. Restituisce {yaw, pitch}; il pitch è limitato a
     * [-90, 90].
     */
    public static double[] deviate(double yaw, double pitch, boolean aiming, RandomGenerator rnd) {
        return deviate(yaw, pitch, aiming, false, rnd);
    }

    /** Come {@link #deviate(double, double, boolean, RandomGenerator)}, con la dispersione ridotta da accovacciati. */
    public static double[] deviate(double yaw, double pitch, boolean aiming, boolean crouching, RandomGenerator rnd) {
        double spread = spreadDegrees(aiming, crouching);
        double theta = rnd.nextDouble() * 2 * Math.PI;
        double radius = Math.toRadians(spread * Math.sqrt(rnd.nextDouble()));
        double yawR = Math.toRadians(yaw);
        double pitchR = Math.toRadians(pitch);
        // Direzione (convenzione Minecraft) e base ortonormale: destra (orizzontale) e su.
        double dx = -Math.sin(yawR) * Math.cos(pitchR);
        double dy = -Math.sin(pitchR);
        double dz = Math.cos(yawR) * Math.cos(pitchR);
        double rx = Math.cos(yawR);
        double rz = Math.sin(yawR);
        double ux = dy * rz;
        double uy = dz * rx - dx * rz;
        double uz = -dy * rx;
        double s = Math.sin(radius);
        double c = Math.cos(radius);
        double ox = Math.cos(theta);
        double oy = Math.sin(theta);
        double nx = dx * c + (rx * ox + ux * oy) * s;
        double ny = dy * c + (uy * oy) * s;
        double nz = dz * c + (rz * ox + uz * oy) * s;
        double newPitch = Math.toDegrees(-Math.asin(Math.max(-1.0, Math.min(1.0, ny))));
        double newYaw = Math.toDegrees(Math.atan2(-nx, nz));
        return new double[] {newYaw, Math.max(-90.0, Math.min(90.0, newPitch))};
    }
}
