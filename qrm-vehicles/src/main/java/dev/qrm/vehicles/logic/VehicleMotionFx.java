package dev.qrm.vehicles.logic;

/**
 * Gli effetti di movimento del solo client: rotazione delle ruote, beccheggio e rollio della cassa. Si avanza di un
 * tick alla volta e si legge interpolato tra il tick precedente e quello corrente. Nessuna dipendenza da Minecraft.
 *
 * <p>Convenzioni: angoli in gradi nel riferimento del modello (davanti = -Z). Beccheggio positivo = muso su, rollio
 * positivo = lato destro su; la rotazione delle ruote è positiva quando il veicolo va avanti.
 */
public final class VehicleMotionFx {

    /** Raggio delle ruote in blocchi: con questa velocità in avanti una ruota fa un radiante per tick. */
    public static final double WHEEL_RADIUS = 0.4;
    public static final double MAX_PITCH_DEG = 3.0;
    public static final double MAX_ROLL_DEG = 4.0;

    private static final double PITCH_DEG_PER_ACCEL = 75.0;
    private static final double SMOOTHING = 0.25;

    private double wheel;
    private double wheelO;
    private double pitch;
    private double pitchO;
    private double roll;
    private double rollO;
    private double prevSpeed;

    /**
     * Avanza di un tick.
     *
     * @param forwardSpeed velocità in avanti in blocchi per tick (negativa in retromarcia)
     * @param steerDeg angolo delle ruote anteriori, positivo a destra
     */
    public void tick(double forwardSpeed, double steerDeg, double maxSpeed, double maxSteerDeg) {
        wheelO = wheel;
        pitchO = pitch;
        rollO = roll;

        wheel += Math.toDegrees(forwardSpeed / WHEEL_RADIUS);

        double accel = forwardSpeed - prevSpeed;
        prevSpeed = forwardSpeed;
        double pitchTarget = clamp(accel * PITCH_DEG_PER_ACCEL, MAX_PITCH_DEG);
        pitch += (pitchTarget - pitch) * SMOOTHING;

        double rollTarget = 0;
        if (maxSpeed > 0 && maxSteerDeg > 0) {
            rollTarget = clamp(steerDeg / maxSteerDeg, 1) * clamp(forwardSpeed / maxSpeed, 1) * MAX_ROLL_DEG;
        }
        roll += (rollTarget - roll) * SMOOTHING;
    }

    public double wheelDeg(float partialTick) {
        return wheelO + (wheel - wheelO) * partialTick;
    }

    public double pitchDeg(float partialTick) {
        return pitchO + (pitch - pitchO) * partialTick;
    }

    public double rollDeg(float partialTick) {
        return rollO + (roll - rollO) * partialTick;
    }

    private static double clamp(double v, double limit) {
        return Math.max(-limit, Math.min(limit, v));
    }
}
