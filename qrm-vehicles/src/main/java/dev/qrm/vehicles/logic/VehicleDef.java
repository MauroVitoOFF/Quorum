package dev.qrm.vehicles.logic;

/**
 * Parametri di un modello di veicolo. Velocità in blocchi per tick, accelerazioni in blocchi per tick², angoli in
 * gradi. Tutti i valori di tuning della guida stanno qui: si regolano provando in gioco.
 */
public record VehicleDef(String id, int seats, double maxSpeed, double reverseMaxSpeed, double accel, double brake,
        double drag, double maxSteerDeg, double steerRateDeg, double grip, double wheelBase, double fuelCapacity,
        double fuelPer100Blocks) {

    public static final VehicleDef JEEP = new VehicleDef("jeep_wrangler", 2, 0.75, 0.25, 0.02, 0.06, 0.005, 30, 3,
            0.35, 2.4, 60, 0.08);
}
