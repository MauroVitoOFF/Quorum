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
    private static final int SUBSTEPS = 20;
    /** Sotto questa velocità (m/s) si usa solo la cinematica di Ackermann; sopra FULL_DYNAMIC solo la dinamica. */
    private static final double KINEMATIC_BELOW = 2.0;
    private static final double FULL_DYNAMIC_ABOVE = 5.0;
    /** Con il freno a mano le ruote posteriori sono bloccate e tengono una frazione dell'attrito. */
    private static final double HANDBRAKE_REAR_GRIP = 0.35;
    /** Quota minima dell'attrito che resta sempre a un asse, anche con la forza longitudinale al limite. */
    private static final double MIN_GRIP_SHARE = 0.05;

    /**
     * Su un fondo con {@code speedFactor} sotto 1, decelerazione (1/s) per ogni m/s oltre la velocità di punta del
     * fondo, fino a un massimo di {@link #SURFACE_SINK_MAX_G}: anche il motore più forte resta vicino alla punta del
     * fondo, ma entrandoci lanciati si rallenta come in una frenata decisa, non come contro un muro.
     */
    private static final double SURFACE_SPEED_DRAG = 6.0;
    private static final double SURFACE_SINK_MAX_G = 0.8;

    /** Pendenza massima considerata (seno), circa 24 gradi: oltre è un gradino, non una salita. */
    public static final double MAX_SLOPE = 0.4;

    /**
     * Quota dell'attrito delle ruote motrici che la spinta può usare. Il resto resta alle gomme per tenere la curva:
     * a 1 il posteriore, spingendo al massimo, non avrebbe più nessuna presa laterale e l'auto girerebbe su sé stessa.
     */
    private static final double TRACTION_UTILISATION = 0.8;

    /**
     * Quanto la forza longitudinale di un asse toglie presa laterale: 1 è il cerchio d'attrito reale, meno è più
     * clemente. Un gioco a tastiera non ha la sensibilità del piede destro, quindi si perdona un po' di sovrapposizione.
     */
    private static final double LONGITUDINAL_COUPLING = 0.5;

    /** Quota dell'aderenza laterale che lo sterzo a fondo può chiedere: sotto 1 resta un margine contro il traverso. */
    private static final double STEER_GRIP_USE = 2.2;

    /**
     * Aderenza laterale in più rispetto a quella di trazione e frenata: in un gioco si vuole curvare più stretto di
     * quanto permetterebbe una gomma vera, senza allungare le frenate.
     */
    public static final double LATERAL_GRIP_BOOST = 2.2;

    /** Quanto forte il muso viene riportato lungo la traiettoria, in rad/s² per radiante di deriva. */
    private static final double STABILITY_GAIN = 25;
    /** Col freno a mano la stabilità cala, così si può mettere l'auto di traverso apposta. */
    private static final double HANDBRAKE_STABILITY = 0.15;

    /** Gas oltre il quale il cambio scala per la ripresa. */
    private static final double KICKDOWN_THROTTLE = 0.85;
    /** Regime massimo (in frazione del regime di salita) a cui si può arrivare scalando. */
    private static final double KICKDOWN_RPM_SHARE = 0.92;

    /** Ampiezza (in quota del regime massimo) della fascia in cui la coppia scende a zero verso il limitatore. */
    private static final double LIMITER_BAND = 0.06;

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
        return step(def, s, rawInput, surface, engineOn, slope, AUTOMATIC);
    }

    /** Valore di {@code selected} che attiva il cambio automatico. */
    public static final int AUTOMATIC = Integer.MIN_VALUE;

    /**
     * Come sopra, con il cambio manuale: {@code selected} è la marcia scelta dal guidatore (-1 retromarcia, 0 folle,
     * da 1 la prima, entro le marce del veicolo). In manuale il gas è sempre in avanti (W) e il comando opposto (S)
     * frena; la retromarcia si ottiene innestando la R. Con {@link #AUTOMATIC} funziona come prima.
     */
    public static DriveState step(VehicleDef def, DriveState s, DriveInput rawInput, Surface surface,
            boolean engineOn, double slope, int selected) {
        boolean manual = selected != AUTOMATIC;
        DriveInput in = rawInput.clamped();
        Powertrain pt = def.powertrain();

        // ---- Sterzata e imbardata (Ackermann) ----
        // Sterzo: le ruote inseguono l'angolo richiesto con una velocità limitata.
        // Più si va veloci, meno le ruote si possono girare: a regime restano il 65% dell'angolo massimo.
        double speedRatio = Math.min(1, Math.abs(s.forwardSpeed()) / def.maxSpeed());
        // L'angolo non supera quello che l'aderenza può trasformare in curva alla velocità attuale (v²·tanθ/L ≤ μ·g):
        // oltre non si curva di più, si va solo di traverso. A bassa velocità il limite non conta.
        double speedMs = Math.max(1, Math.abs(s.forwardSpeed()) * TPS);
        // In arcade lo sterzo resta un po' sotto il limite delle gomme, così gas e freni non lo portano oltre.
        double gripUse = def.handling() == HandlingModel.ARCADE ? ARCADE_GRIP * ARCADE_STEER_MARGIN
                : STEER_GRIP_USE * LATERAL_GRIP_BOOST;
        double gripLimitDeg = Math.toDegrees(Math.atan(
                gripUse * pt.tyreMu() * surface.gripFactor() * Powertrain.GRAVITY * def.wheelBase()
                        / (speedMs * speedMs)));
        double target = in.steer() * Math.min(def.maxSteerDeg() * (1 - 0.35 * speedRatio), gripLimitDeg);
        double steer = s.steerAngle() + Math.max(-def.steerRateDeg(), Math.min(def.steerRateDeg(), target - s.steerAngle()));
        boolean classic = def.handling() == HandlingModel.CLASSIC;
        if (classic) steer = ClassicHandling.steerAngle(s.steerAngle(), in.steer());

        // Il muso e il lato (sinistro) del veicolo nel mondo, all'inizio del tick.
        double h = Math.toRadians(s.heading());
        double vF = s.vx() * -Math.sin(h) + s.vz() * Math.cos(h);
        double vL = s.vx() * Math.cos(h) + s.vz() * Math.sin(h);

        // ---- Marcia e pedali ----
        double v = vF * TPS; // m/s lungo il muso, negativa in retromarcia
        int gear;
        double drive = 0;
        double brakePedal = 0;
        if (manual) {
            gear = Math.max(-1, Math.min(pt.gears(), selected));
            drive = Math.max(0, in.throttle());
            brakePedal = Math.max(0, -in.throttle());
        } else {
            gear = s.gear() == 0 ? 1 : s.gear();
            if (in.throttle() < 0 && v < REVERSE_ENGAGE_SPEED) {
                gear = -1;
            } else if (in.throttle() > 0 && v > -REVERSE_ENGAGE_SPEED && gear < 0) {
                gear = 1;
            }
            // Il gas spinge nel verso della marcia; il comando opposto mentre si va nell'altro verso è il freno.
            if (gear > 0) {
                drive = Math.max(0, in.throttle());
                if (in.throttle() < 0 && v > REVERSE_ENGAGE_SPEED) brakePedal = -in.throttle();
            } else {
                drive = Math.max(0, -in.throttle());
                if (in.throttle() > 0 && v < -REVERSE_ENGAGE_SPEED) brakePedal = in.throttle();
            }
        }
        if (in.brake()) brakePedal = HANDBRAKE_FACTOR;

        // ---- Cambio automatico ----
        // Dai giri della ruota: regime = giri ruota × rapporto. Si sale oltre shiftUp e si scala sotto shiftDown; la
        // distanza tra le due soglie è più larga del salto tra marce, quindi non si oscilla.
        double wheelRpm = Math.abs(v) / pt.wheelRadius() * 60.0 / (2 * Math.PI);
        if (!manual && gear > 0) {
            while (wheelRpm * pt.ratio(gear) > pt.shiftUpRpm() && gear < pt.gears()) gear++;
            while (wheelRpm * pt.ratio(gear) < pt.shiftDownRpm() && gear > 1) gear--;
            // Kickdown: a gas quasi a fondo si scala finché il motore resta sotto la soglia di cambiata, per avere
            // la coppia dove serve. Sotto il regime di salita, così non si risale subito.
            if (drive >= KICKDOWN_THROTTLE) {
                while (gear > 1 && wheelRpm * pt.ratio(gear - 1) <= pt.shiftUpRpm() * KICKDOWN_RPM_SHARE) gear--;
            }
        }
        double ratio = gear == 0 ? 0 : pt.ratio(gear);
        double gearRpm = wheelRpm * ratio;

        // ---- Motore: regime, coppia e forza alla ruota ----
        double mu = pt.tyreMu() * surface.gripFactor();
        double weight = pt.massKg() * Powertrain.GRAVITY; // N
        double driveForce = 0;
        double rpmOut = 0;
        if (engineOn && gear == 0) {
            // Folle: il motore gira libero dietro al gas, senza spingere né frenare.
            double free = pt.idleRpm() + drive * (pt.redlineRpm() - pt.idleRpm()) * 0.85;
            double from = Math.max(pt.idleRpm(), s.rpm());
            rpmOut = Math.min(pt.redlineRpm(), from + (free - from) * 0.25);
        } else if (engineOn) {
            double rpm = Math.max(pt.idleRpm(), gearRpm);
            if (drive > 0) rpm = Math.max(rpm, pt.launchRpm() * drive); // a veicolo lento la frizione slitta
            rpmOut = Math.min(rpm, pt.redlineRpm());
            if (drive > 0) {
                // Coppia dalla curva, a gas parziale proporzionale; oltre il limitatore il motore non spinge.
                // Il limitatore è morbido: la coppia cala negli ultimi giri invece di staccarsi di colpo, altrimenti a
                // fondo scala il motore rimbalza sul limite e la velocità pulsa.
                double limiter = Math.max(0, Math.min(1, (pt.redlineRpm() - rpm) / (LIMITER_BAND * pt.redlineRpm())));
                double torque = pt.torqueAt(rpm) * drive * limiter;
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
            double tractionLimit = TRACTION_UTILISATION * mu * weight * pt.driveWeightFraction();
            driveForce = Math.max(-tractionLimit, Math.min(tractionLimit, driveForce));
        }

        // ---- Resistenze ----
        // Aria: ½ · ρ · Cd·A · v²   Rotolamento: Crr · m · g (cresce su terra e fondo difficile)
        double drag = 0.5 * Powertrain.AIR_DENSITY * pt.dragArea() * v * v;
        double rolling = pt.rollingResistance() * surface.rollFactor() * weight;
        // Fondo lento (terra, sabbia, neve): oltre la sua velocità di punta (maxSpeed · speedFactor) le ruote
        // affondano e una resistenza proporzionale all'eccesso riporta giù la velocità senza strappi.
        double surfaceCap = def.maxSpeed() * TPS * surface.speedFactor();
        double sink = surface.speedFactor() < 1 && Math.abs(v) > surfaceCap
                ? Math.min(pt.massKg() * SURFACE_SPEED_DRAG * (Math.abs(v) - surfaceCap), SURFACE_SINK_MAX_G * weight)
                : 0;
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
        double dv = (drag + rolling + sink + brakeForce) / pt.massKg() * DT;
        double v2 = Math.abs(v1) <= dv ? 0 : v1 - Math.signum(v1) * dv;
        vF = v2 / TPS;

        // ---- Dinamica laterale ----
        // Forze longitudinali di questo tick: tolgono attrito alle gomme che le trasmettono (cerchio d'attrito).
        double frontShare = pt.frontDriveShare();
        double axleFxFront = Math.abs(driveForce) * frontShare + brakeForce * pt.frontWeight();
        double axleFxRear = Math.abs(driveForce) * (1 - frontShare) + brakeForce * (1 - pt.frontWeight());
        double[] lateral = classic
                ? ClassicHandling.lateral(v2, vL * TPS, steer, def.wheelBase(), surface.gripFactor(), in.brake())
                : def.handling() == HandlingModel.ARCADE
                ? arcadeLateral(def, mu, v2, vL * TPS, s.yawRate(), Math.toRadians(steer),
                        Math.abs(driveForce), brakeForce, in.brake())
                : lateralDynamics(def, mu * LATERAL_GRIP_BOOST, v2, vL * TPS, s.yawRate(), Math.toRadians(steer),
                        axleFxFront, axleFxRear, in.brake());
        double heading = s.heading() + Math.toDegrees(lateral[2] * DT);
        double hNew = Math.toRadians(heading);
        vF = lateral[0] / TPS;
        vL = lateral[1] / TPS;

        // Di nuovo nel mondo, con il muso girato.
        double vx = vF * -Math.sin(hNew) + vL * Math.cos(hNew);
        double vz = vF * Math.cos(hNew) + vL * Math.sin(hNew);
        return new DriveState(heading, steer, vx, vz, gear, rpmOut, lateral[2]);
    }

    /** Aderenza laterale massima dell'arcade, in multipli di μ·g: la stessa che limita l'angolo delle ruote. */
    private static final double ARCADE_GRIP = 3.0;
    /** Quota del limite di aderenza che lo sterzo a fondo può chiedere in arcade. */
    private static final double ARCADE_STEER_MARGIN = 0.92;
    /** Col freno a mano il posteriore tiene poco e l'auto si mette di traverso. */
    private static final double ARCADE_HANDBRAKE_GRIP = 0.35;
    /** Con il freno a mano il muso gira di più, per far partire la derapata. */
    private static final double ARCADE_HANDBRAKE_YAW = 1.4;
    /** Quota di velocità persa per radiante di traiettoria raddrizzata dalle gomme. */
    private static final double ARCADE_SCRUB = 0.05;
    /** Il muso segue lo sterzo con un po' d'inerzia: quota del divario colmata a ogni tick. */
    private static final double ARCADE_YAW_RESPONSE = 0.5;
    /** Quanto il gas pesa sull'aderenza (più della dinamica: rilascia il posteriore in uscita di curva). */
    private static final double ARCADE_DRIVE_COUPLING = 0.6;
    /** Frenando in curva il peso va avanti: il muso gira di più, fino a questa quota in più. */
    private static final double ARCADE_TRAIL_BRAKE_YAW = 0.2;
    /** Aderenza che resta sempre, anche con gas o freno a fondo. */
    private static final double ARCADE_MIN_GRIP_SHARE = 0.3;

    /**
     * Guida arcade. Il muso gira per sola geometria (Ackermann): non serve alcuna dinamica per sentire lo sterzo
     * immediato. La velocità resta quella di prima nel mondo, quindi rispetto al nuovo muso compare una componente
     * laterale; le gomme la tolgono ruotando la velocità verso il muso, ma al massimo di quanto concede
     * l'aderenza del tick (ridotta da gas e freni, dal fondo e dal freno a mano). Quel che avanza è scivolamento.
     *
     * @return {u finale, vy finale, r} in m/s, m/s, rad/s (r positiva a destra)
     */
    private static double[] arcadeLateral(VehicleDef def, double mu, double u0, double vy0, double yaw0,
            double steerRad, double driveForce, double brakeForce, boolean handbrake) {
        Powertrain pt = def.powertrain();
        double weight = pt.massKg() * Powertrain.GRAVITY;
        double brakeUse = Math.min(1, brakeForce / (mu * weight));
        double rTarget = u0 / def.wheelBase() * Math.tan(steerRad) * (handbrake ? ARCADE_HANDBRAKE_YAW : 1)
                * (1 + ARCADE_TRAIL_BRAKE_YAW * brakeUse);
        double r = yaw0 + (rTarget - yaw0) * ARCADE_YAW_RESPONSE;
        double d = r * DT;
        // La stessa velocità del mondo, espressa nel muso girato di d.
        double u = u0 * Math.cos(d) - vy0 * Math.sin(d);
        double w = vy0 * Math.cos(d) + u0 * Math.sin(d);

        double use = Math.min(1, (ARCADE_DRIVE_COUPLING * driveForce + LONGITUDINAL_COUPLING * brakeForce)
                / (mu * weight));
        double share = Math.max(ARCADE_MIN_GRIP_SHARE, Math.sqrt(1 - use * use));
        double gripAccel = ARCADE_GRIP * mu * Powertrain.GRAVITY * share * (handbrake ? ARCADE_HANDBRAKE_GRIP : 1);
        double corr = Math.min(Math.abs(w), gripAccel * DT);
        if (corr > 0) {
            double speed = Math.hypot(u, w);
            double w2 = w - Math.signum(w) * corr;
            double u2 = Math.signum(u == 0 ? u0 : u) * Math.sqrt(Math.max(0, speed * speed - w2 * w2));
            u2 *= 1 - ARCADE_SCRUB * corr / Math.max(speed, 2);
            u = u2;
            w = w2;
        }
        return new double[] {u, w, r};
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
        double stability = handbrake ? HANDBRAKE_STABILITY : 1;
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
            // Stabilità da gioco: un momento che riporta il muso nella direzione di marcia, come il "galletto" di un'auto
            // vera ma anche quando il posteriore ha perso presa. Senza, con le ruote dritte l'auto continua a ruotare.
            double realign = -STABILITY_GAIN * stability * Math.atan(vy / uEff);
            double rDot = (-a * fyFront + b * fyRear) / yawInertia + realign;
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
        double coupled = LONGITUDINAL_COUPLING * fx;
        double rest = friction * friction - coupled * coupled;
        return Math.max(MIN_GRIP_SHARE * friction, Math.sqrt(Math.max(0, rest)));
    }
}
