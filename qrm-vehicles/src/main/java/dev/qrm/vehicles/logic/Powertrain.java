package dev.qrm.vehicles.logic;

/**
 * Motore, cambio e dati di massa di un veicolo, in unità SI (kg, N, Nm, m, giri/min). Sono i parametri che la fisica
 * di {@link DrivingModel} usa per trasformare l'acceleratore in forza alle ruote. Un blocco vale un metro. Le coppie
 * e i rapporti sono tarati sul gioco, non su un'auto vera: le velocità massime restano sotto circa 1 blocco per tick.
 *
 * @param massKg massa del veicolo con il pilota
 * @param wheelRadius raggio della ruota motrice, in metri
 * @param rpm punti della curva di coppia (giri/min, crescenti)
 * @param torqueNm coppia del motore a quei giri, a gas pieno
 * @param idleRpm regime al minimo
 * @param redlineRpm limitatore: oltre questo regime il motore non spinge
 * @param launchRpm regime a cui lavora il motore a veicolo fermo (la frizione slitta)
 * @param shiftUpRpm la marcia sale oltre questo regime
 * @param shiftDownRpm la marcia scala sotto questo regime
 * @param gearRatios rapporti delle marce avanti, dalla prima all'ultima (compreso il rapporto finale)
 * @param reverseRatio rapporto della retromarcia
 * @param efficiency rendimento della trasmissione, 0..1
 * @param engineBrakeNm coppia frenante del motore al regime massimo quando si lascia il gas
 * @param dragArea Cd·A, area frontale effettiva in m²
 * @param rollingResistance coefficiente di resistenza al rotolamento su asfalto
 * @param driveWeightFraction quota del peso sulle ruote motrici (limita la trazione)
 * @param brakeG decelerazione massima dei freni, in multipli di g
 * @param tyreMu coefficiente di attrito delle gomme su asfalto
 * @param frontWeight quota del peso sull'asse anteriore (0..1): decide la posizione del baricentro
 * @param frontDriveShare quota della trazione sull'asse anteriore: 0 posteriore, 0,5 integrale
 * @param frontCornering rigidezza di deriva dell'asse anteriore per unità di carico (1/rad): forza = coeff · carico · angolo
 * @param rearCornering come sopra per il posteriore: più alta dell'anteriore dà sottosterzo, più bassa sovrasterzo
 * @param yawGyration raggio di girazione in frazione del passo: l'inerzia di imbardata è massa · (raggio · passo)²
 */
public record Powertrain(double massKg, double wheelRadius, double[] rpm, double[] torqueNm, double idleRpm,
        double redlineRpm, double launchRpm, double shiftUpRpm, double shiftDownRpm, double[] gearRatios,
        double reverseRatio, double efficiency, double engineBrakeNm, double dragArea, double rollingResistance,
        double driveWeightFraction, double brakeG, double tyreMu, double frontWeight, double frontDriveShare,
        double frontCornering, double rearCornering, double yawGyration) {

    public static final double GRAVITY = 9.81;
    /** Densità dell'aria in kg/m³. */
    public static final double AIR_DENSITY = 1.2;

    public Powertrain {
        if (rpm.length != torqueNm.length || rpm.length < 2) throw new IllegalArgumentException("curva di coppia");
        if (gearRatios.length < 1) throw new IllegalArgumentException("nessuna marcia");
    }

    public int gears() {
        return gearRatios.length;
    }

    /** Rapporto totale tra motore e ruota per la marcia (1..n avanti, -1 retromarcia). */
    public double ratio(int gear) {
        return gear < 0 ? reverseRatio : gearRatios[Math.min(gear, gearRatios.length) - 1];
    }

    /** Coppia a gas pieno a questo regime, interpolata sui punti della curva. */
    public double torqueAt(double engineRpm) {
        if (engineRpm <= rpm[0]) return torqueNm[0];
        for (int i = 1; i < rpm.length; i++) {
            if (engineRpm <= rpm[i]) {
                double t = (engineRpm - rpm[i - 1]) / (rpm[i] - rpm[i - 1]);
                return torqueNm[i - 1] + (torqueNm[i] - torqueNm[i - 1]) * t;
            }
        }
        return torqueNm[torqueNm.length - 1];
    }

    private static double[] scaled(double peak, double... shape) {
        double[] out = new double[shape.length];
        for (int i = 0; i < shape.length; i++) out[i] = shape[i] * peak;
        return out;
    }

    /** Quattro marce, trazione posteriore, muscle car: velocità massima di progetto 17 m/s. */
    public static final Powertrain CHARGER = new Powertrain(1700, 0.4875,
            new double[] {1000, 2000, 3000, 4000, 5000, 6000}, scaled(92, 0.62, 0.8, 0.95, 1, 0.93, 0.78), 800, 6000,
            2500, 5400, 2700, new double[] {43.24, 32.30, 24.12, 18.02}, 60.5, 0.9, 40, 0.75, 0.03, 0.55, 0.9, 1.0, 0.52, 0, 8, 11, 0.33);

    /** Fuoristrada con trazione integrale: più peso sulle ruote motrici, più resistenza all'aria. */
    public static final Powertrain JEEP = new Powertrain(1500, 0.40625,
            new double[] {1000, 2000, 3000, 4000, 5000}, scaled(72, 0.7, 0.9, 1, 0.92, 0.75), 700, 5000, 2000, 4500,
            2250, new double[] {34.03, 25.42, 18.99, 14.18}, 47.7, 0.88, 35, 1.1, 0.035, 0.9, 0.85, 1.0, 0.52, 0.5, 7, 9, 0.33);

}
