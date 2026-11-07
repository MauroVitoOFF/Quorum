package dev.qrm.vehicles.logic;

/**
 * Tono e volume del suono del motore a partire dai giri. Il suono registrato è il minimo: il tono parte da 1 e sale
 * fino a {@link #MAX_PITCH} al limitatore (Minecraft accetta da 0,5 a 2), il volume cresce col carico.
 */
public final class EngineSoundMath {
    /** Massimo tono accettato dal gioco. */
    public static final double MAX_PITCH = 2.0;
    /** Quanto il tono si avvicina al valore voluto a ogni tick (0..1). */
    public static final double SMOOTHING = 0.35;
    private static final double MIN_VOLUME = 0.55;
    private static final double MAX_VOLUME = 1.0;

    /** Distanza (in blocchi) oltre la quale il motore non si sente più. */
    public static final double HEARING_RANGE = 48.0;

    private EngineSoundMath() {}

    /**
     * Quanto si sente il motore a una certa distanza: 1 accanto al veicolo, 0 oltre {@code range}, con una discesa
     * morbida in mezzo. Serve perché un suono stereo non viene attenuato dal gioco.
     */
    public static double distanceGain(double distance, double range) {
        if (Double.isNaN(distance) || !(range > 0)) return 0;
        if (distance <= 0) return 1;
        if (distance >= range) return 0;
        double left = 1 - distance / range;
        return left * left;
    }

    private static double load(double rpm, Powertrain pt) {
        double span = pt.redlineRpm() - pt.idleRpm();
        if (!(span > 0) || Double.isNaN(rpm)) return 0;
        return Math.max(0, Math.min(1, (rpm - pt.idleRpm()) / span));
    }

    public static double pitch(double rpm, Powertrain pt) {
        return 1.0 + (MAX_PITCH - 1.0) * load(rpm, pt);
    }

    public static double volume(double rpm, Powertrain pt) {
        return MIN_VOLUME + (MAX_VOLUME - MIN_VOLUME) * load(rpm, pt);
    }
}
