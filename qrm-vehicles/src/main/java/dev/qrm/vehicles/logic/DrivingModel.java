package dev.qrm.vehicles.logic;

/**
 * Il modello di guida, un tick alla volta e senza dipendere da Minecraft. Il moto in avanti si ottiene integrando le
 * forze (trazione del motore attraverso cambio automatico, resistenza dell'aria, rotolamento, freni); lo sterzo è la
 * geometria di Ackermann a bicicletta; sopra pochi m/s la dinamica laterale è quella del modello a bicicletta: ogni
 * asse genera una forza proporzionale all'angolo di deriva delle gomme, saturata dall'attrito disponibile (sotto- e
 * sovrasterzo, controsterzo, derapata col freno a mano). Calcoli in unità SI: la velocità
 * dello stato è in blocchi per tick (1 blocco = 1 m, 20 tick al secondo) e si converte solo all'ingresso e all'uscita.
 */
public final class DrivingModel {

    /** Tick al secondo del gioco. */
    public static final double TPS = 20.0;
    private static final double DT = 1.0 / TPS;

    /** Sotto questa velocità (m/s) il veicolo si considera fermo per scegliere tra avanti e retromarcia. */
    private static final double REVERSE_ENGAGE_SPEED = 0.3;
    /** Il freno a mano frena più forte del pedale: può bloccare le ruote anche sull'asfalto. */
    private static final double HANDBRAKE_FACTOR = 1.3;
    /** A ruote bloccate si frena col solo attrito dinamico, un po' meno di quello statico. */
    private static final double LOCKED_FRICTION = 0.85;

    /** Sottopassi della dinamica laterale per tick: le gomme sono rigide e l'integrazione esplicita va tenuta stabile. */
    private static final int SUBSTEPS = 10;
    /** Sotto questa velocità (m/s) si usa solo la cinematica di Ackermann; sopra FULL_DYNAMIC solo la dinamica. */
    private static final double KINEMATIC_BELOW = 2.0;
    private static final double FULL_DYNAMIC_ABOVE = 5.0;
    /** Con il freno a mano le ruote posteriori sono bloccate e tengono una frazione dell'attrito. */
    private static final double HANDBRAKE_REAR_GRIP = 0.35;
    /** Quota minima dell'attrito che resta sempre a un asse, anche con la forza longitudinale al limite. */
    private static final double MIN_GRIP_SHARE = 0.05;

    /** Pendenza massima considerata (seno), circa 24 gradi: oltre è un gradino, non una salita. */
    public static final double MAX_SLOPE = 0.4;

    private DrivingModel() {}

    /** Un tick su terreno piano. */
    public static DriveState step(VehicleDef def, DriveState s, DriveInput rawInput, Surface surface,
            boolean engineOn) {
        return step(def, s, rawInput, surface, engineOn, 0);
    }

    /**
     * Un tick. {@code slope} è il seno dell'inclinazione del terreno nel verso del muso (positivo in salita): la
     * gravità aggiunge la forza m · g · slope contro (o a favore) del moto, e la stessa componente tiene fermo il
     * veicolo se l'attrito di rotolamento o i freni la bilanciano.
     */
    public static DriveState step(VehicleDef def, DriveState s, DriveInput rawInput, Surface surface,
            boolean engineOn, double slope) {
        DriveInput in = rawInput.clamped();
        Powertrain pt = def.powertrain();

        // ---- Sterzata e imbardata (Ackermann) ----
        // Sterzo: le ruote inseguono l'angolo richiesto con una velocità limitata.
        // Più si va veloci, meno le ruote si possono girare: a regime restano il 40% dell'angolo massimo.
        double speedRatio = Math.min(1, Math.abs(s.forwardSpeed()) / def.maxSpeed());
        double target = in.steer() * def.maxSteerDeg() * (1 - 0.6 * speedRatio);
        double steer = s.steerAngle() + Math.max(-def.steerRateDeg(), Math.min(def.steerRateDeg(), target - s.steerAngle()));

        // Il muso e il lato (sinistro) del veicolo nel mondo, all'inizio del tick.
        double h = Math.toRadians(s.heading());
        double vF = s.vx() * -Math.sin(h) + s.vz() * Math.cos(h);
        double vL = s.vx() * Math.cos(h) + s.vz() * Math.sin(h);

        // ---- Marcia e pedali ----
        double v = vF * TPS; // m/s lungo il muso, negativa in retromarcia
        int gear = s.gear() == 0 ? 1 : s.gear();
        if (in.throttle() < 0 && v < REVERSE_ENGAGE_SPEED) {
            gear = -1;
        } else if (in.throttle() > 0 && v > -REVERSE_ENGAGE_SPEED && gear < 0) {
            gear = 1;
        }
        // Il gas spinge nel verso della marcia; il comando opposto mentre si va nell'altro verso è il freno.
        double drive = 0;
        double brakePedal = 0;
        if (gear > 0) {
            drive = Math.max(0, in.throttle());
            if (in.throttle() < 0 && v > REVERSE_ENGAGE_SPEED) brakePedal = -in.throttle();
        } else {
            drive = Math.max(0, -in.throttle());
            if (in.throttle() > 0 && v < -REVERSE_ENGAGE_SPEED) brakePedal = in.throttle();
        }
        if (in.brake()) brakePedal = HANDBRAKE_FACTOR;

        // ---- Cambio automatico ----
        // Dai giri della ruota: regime = giri ruota × rapporto. Si sale oltre shiftUp e si scala sotto shiftDown; la
        // distanza tra le due soglie è più larga del salto tra marce, quindi non si oscilla.
        double wheelRpm = Math.abs(v) / pt.wheelRadius() * 60.0 / (2 * Math.PI);
        if (gear > 0) {
            while (wheelRpm * pt.ratio(gear) > pt.shiftUpRpm() && gear < pt.gears()) gear++;
            while (wheelRpm * pt.ratio(gear) < pt.shiftDownRpm() && gear > 1) gear--;
        }
        double ratio = pt.ratio(gear);
        double gearRpm = wheelRpm * ratio;

        // ---- Motore: regime, coppia e forza alla ruota ----
        double mu = pt.tyreMu() * surface.gripFactor();
        double weight = pt.massKg() * Powertrain.GRAVITY; // N
        double driveForce = 0;
        double rpmOut = 0;
        if (engineOn) {
            double rpm = Math.max(pt.idleRpm(), gearRpm);
            if (drive > 0) rpm = Math.max(rpm, pt.launchRpm() * drive); // a veicolo lento la frizione slitta
            rpmOut = Math.min(rpm, pt.redlineRpm());
            if (drive > 0) {
                // Coppia dalla curva, a gas parziale proporzionale; oltre il limitatore il motore non spinge.
                double torque = rpm >= pt.redlineRpm() ? 0 : pt.torqueAt(rpm) * drive;
                // F = coppia · rapporto · rendimento / raggio ruota
                double f = torque * ratio * pt.efficiency() / pt.wheelRadius();
                driveForce = (gear > 0 ? 1 : -1) * f;
            } else if (v != 0) {
                // Freno motore: a gas chiuso il motore rallenta il veicolo in proporzione ai giri.
                double load = Math.max(0, Math.min(1,
                        (gearRpm - pt.idleRpm()) / (pt.redlineRpm() - pt.idleRpm())));
                driveForce = -Math.signum(v) * pt.engineBrakeNm() * load * ratio * pt.efficiency() / pt.wheelRadius();
            }
            // Trazione: oltre l'attrito delle ruote motrici le gomme pattinano e la forza non cresce.
            double tractionLimit = mu * weight * pt.driveWeightFraction();
            driveForce = Math.max(-tractionLimit, Math.min(tractionLimit, driveForce));
        }

        // ---- Resistenze ----
        // Aria: ½ · ρ · Cd·A · v²   Rotolamento: Crr · m · g (cresce su terra e fondo difficile)
        double drag = 0.5 * Powertrain.AIR_DENSITY * pt.dragArea() * v * v;
        double rolling = pt.rollingResistance() * surface.rollFactor() * weight;
        // Freni: forza richiesta = pedale · decelerazione massima · m · g. Oltre l'attrito totale le ruote si
        // bloccano e si frena con l'attrito dinamico (spazio di frenata più lungo).
        double brakeForce = 0;
        if (brakePedal > 0) {
            double requested = brakePedal * pt.brakeG() * weight;
            double frictionLimit = mu * weight;
            brakeForce = requested > frictionLimit ? LOCKED_FRICTION * frictionLimit : requested;
        }

        // ---- Integrazione: a = F / m ----
        // Prima la spinta, poi le resistenze che si oppongono al moto ma non lo invertono (si fermano a zero).
        double gravityForce = -weight * Math.max(-MAX_SLOPE, Math.min(MAX_SLOPE, Double.isFinite(slope) ? slope : 0));
        double v1 = v + (driveForce + gravityForce) / pt.massKg() * DT;
        double dv = (drag + rolling + brakeForce) / pt.massKg() * DT;
        double v2 = Math.abs(v1) <= dv ? 0 : v1 - Math.signum(v1) * dv;
        vF = v2 / TPS;

        // ---- Dinamica laterale ----
        // Forze longitudinali di questo tick: tolgono attrito alle gomme che le trasmettono (cerchio d'attrito).
        double frontShare = pt.frontDriveShare();
        double axleFxFront = Math.abs(driveForce) * frontShare + brakeForce * pt.frontWeight();
        double axleFxRear = Math.abs(driveForce) * (1 - frontShare) + brakeForce * (1 - pt.frontWeight());
        double[] lateral = lateralDynamics(def, mu, v2, vL * TPS, s.yawRate(), Math.toRadians(steer), axleFxFront,
                axleFxRear, in.brake());
        double heading = s.heading() + Math.toDegrees(lateral[2] * DT);
        double hNew = Math.toRadians(heading);
        vF = lateral[0] / TPS;
        vL = lateral[1] / TPS;

        // Di nuovo nel mondo, con il muso girato.
        double vx = vF * -Math.sin(hNew) + vL * Math.cos(hNew);
        double vz = vF * Math.cos(hNew) + vL * Math.sin(hNew);
        return new DriveState(heading, steer, vx, vz, gear, rpmOut, lateral[2]);
    }

    /**
     * Modello a bicicletta. Riferimento del corpo: avanti = u, sinistra = vy, imbardata r positiva a destra (come lo
     * sterzo). Ogni asse produce Fy = -Fmax · tanh(k · deriva / Fmax) con Fmax = √((μ·N)² − Fx²): lineare per derive
     * piccole, poi satura. Sotto KINEMATIC_BELOW la deriva non è definita e vale la sola geometria di Ackermann.
     *
     * @return {u finale, vy finale, r finale} in m/s, m/s, rad/s
     */
    private static double[] lateralDynamics(VehicleDef def, double mu, double u0, double vy0, double r0,
            double steerRad, double fxFront, double fxRear, boolean handbrake) {
        Powertrain pt = def.powertrain();
        double wheelBase = def.wheelBase();
        double rKinematic = u0 / wheelBase * Math.tan(steerRad);
        if (u0 <= 1.0) {
            // Quasi fermo o in retro: niente deriva, la velocità laterale si smorza e si gira solo per geometria.
            return new double[] {u0, vy0 * 0.5, rKinematic};
        }
        double weight = pt.massKg() * Powertrain.GRAVITY;
        double nFront = weight * pt.frontWeight();
        double nRear = weight - nFront;
        double a = wheelBase * (1 - pt.frontWeight()); // baricentro - asse anteriore
        double b = wheelBase * pt.frontWeight(); // baricentro - asse posteriore
        double yawInertia = pt.massKg() * Math.pow(pt.yawGyration() * wheelBase, 2);
        double maxFront = axleLimit(mu * nFront, fxFront);
        double maxRear = axleLimit(mu * nRear, fxRear) * (handbrake ? HANDBRAKE_REAR_GRIP : 1);
        double dt = DT / SUBSTEPS;
        double sinSteer = Math.sin(steerRad);
        double cosSteer = Math.cos(steerRad);

        double u = u0;
        double vy = vy0;
        double r = r0;
        double rSum = 0;
        for (int i = 0; i < SUBSTEPS; i++) {
            double uEff = Math.max(u, KINEMATIC_BELOW);
            // Deriva: angolo tra la velocità del punto della gomma e la direzione in cui punta (sinistra positiva).
            double slipFront = Math.atan((vy - a * r) / uEff) + steerRad;
            double slipRear = Math.atan((vy + b * r) / uEff);
            double fyWheel = -maxFront * Math.tanh(pt.frontCornering() * nFront * slipFront / maxFront);
            double fyRear = -maxRear * Math.tanh(pt.rearCornering() * nRear * slipRear / maxRear);
            // La ruota sterzata spinge perpendicolare a sé: una parte della forza laterale frena il veicolo.
            double fyFront = fyWheel * cosSteer;
            double scrub = fyWheel * sinSteer;
            double vyDot = (fyFront + fyRear) / pt.massKg() + u * r;
            double rDot = (-a * fyFront + b * fyRear) / yawInertia;
            double uDot = -vy * r + scrub / pt.massKg();
            u += uDot * dt;
            vy += vyDot * dt;
            r += rDot * dt;
            rSum += r;
        }
        double w = Math.max(0, Math.min(1, (u - KINEMATIC_BELOW) / (FULL_DYNAMIC_ABOVE - KINEMATIC_BELOW)));
        w = w * w * (3 - 2 * w);
        double rDynamic = rSum / SUBSTEPS;
        double rOut = w * rDynamic + (1 - w) * rKinematic;
        return new double[] {u, vy * (w + (1 - w) * 0.5), rOut};
    }

    /** L'attrito che resta a un asse che già trasmette la forza longitudinale fx (mai meno di una quota minima). */
    private static double axleLimit(double friction, double fx) {
        double rest = friction * friction - fx * fx;
        return Math.max(MIN_GRIP_SHARE * friction, Math.sqrt(Math.max(0, rest)));
    }
}
