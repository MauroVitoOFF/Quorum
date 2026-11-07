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
    public static final double MAX_ROLL_DEG = 3.0;

    /** Massimo spostamento verticale della cassa sulle molle, in unità del modello (1/16 di blocco). */
    public static final double MAX_HEAVE_PX = 2.5;

    private static final double PITCH_DEG_PER_ACCEL = 75.0;
    /** Molla della cassa (per tick²) e smorzamento (per tick): oscilla una volta e si assesta in circa mezzo secondo. */
    private static final double SPRING = 0.30;
    private static final double DAMPING = 0.45;
    /** Lo sterzo arriva dal server a scalini di mezzo grado: si livella e si interpola, così il volante scorre. */
    private static final double STEER_SMOOTHING = 0.3;
    /** Quanto un cambio di velocità verticale (gradino, dosso, atterraggio) scuote la cassa. */
    private static final double HEAVE_KICK_PX = 20.0;

    private double wheel;
    private double wheelO;
    private double pitch;
    private double pitchO;
    private double roll;
    private double rollO;
    private double prevSpeed;
    private double pitchV;
    private double rollV;
    private double heave;
    private double heaveO;
    private double heaveV;
    private double prevVertical;
    private double steer;
    private double steerO;

    /**
     * Avanza di un tick.
     *
     * @param forwardSpeed velocità in avanti in blocchi per tick (negativa in retromarcia)
     * @param steerDeg angolo delle ruote anteriori, positivo a destra
     */
    public void tick(double forwardSpeed, double steerDeg, double maxSpeed, double maxSteerDeg) {
        tick(forwardSpeed, steerDeg, maxSpeed, maxSteerDeg, 0);
    }

    /**
     * Come {@link #tick(double, double, double, double)} con la velocità verticale in blocchi per tick: i cambi
     * improvvisi (gradini, dossi, atterraggi) fanno rimbalzare la cassa sulle sospensioni.
     */
    public void tick(double forwardSpeed, double steerDeg, double maxSpeed, double maxSteerDeg, double verticalSpeed) {
        wheelO = wheel;
        pitchO = pitch;
        rollO = roll;
        heaveO = heave;
        steerO = steer;
        steer += (steerDeg - steer) * STEER_SMOOTHING;

        wheel += Math.toDegrees(forwardSpeed / WHEEL_RADIUS);

        double accel = forwardSpeed - prevSpeed;
        prevSpeed = forwardSpeed;
        double pitchTarget = clamp(accel * PITCH_DEG_PER_ACCEL, MAX_PITCH_DEG);
        // La cassa è una massa su molla: raggiunge l'assetto voluto con un piccolo rimbalzo, non di colpo.
        pitchV += (pitchTarget - pitch) * SPRING - pitchV * DAMPING;
        pitch = clamp(pitch + pitchV, MAX_PITCH_DEG * 1.3);

        double rollTarget = 0;
        if (maxSpeed > 0 && maxSteerDeg > 0) {
            rollTarget = clamp(steerDeg / maxSteerDeg, 1) * clamp(forwardSpeed / maxSpeed, 1) * MAX_ROLL_DEG;
        }
        rollV += (rollTarget - roll) * SPRING - rollV * DAMPING;
        roll = clamp(roll + rollV, MAX_ROLL_DEG * 1.3);

        double vertical = Double.isFinite(verticalSpeed) ? verticalSpeed : 0;
        double kick = vertical - prevVertical;
        prevVertical = vertical;
        // La ruota sale col gradino e la cassa, per inerzia, resta indietro (e viceversa all'atterraggio).
        heaveV += -heave * SPRING - heaveV * DAMPING - kick * HEAVE_KICK_PX;
        heave = clamp(heave + heaveV, MAX_HEAVE_PX);
    }

    /** Spostamento verticale della cassa in unità del modello (positivo = su). */
    public double heavePx(float partialTick) {
        return heaveO + (heave - heaveO) * partialTick;
    }

    /** Angolo di sterzo livellato e interpolato, in gradi (positivo a destra). */
    public double steerDeg(float partialTick) {
        return steerO + (steer - steerO) * partialTick;
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
