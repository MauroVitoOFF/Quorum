package dev.qrm.vehicles.logic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parametri di un modello di veicolo. {@code maxSpeed} è la velocità di progetto in blocchi per tick (serve a sterzo,
 * HUD ed effetti), gli angoli sono in gradi; massa, motore e forze stanno in {@link Powertrain}. Tutti i valori di
 * tuning della guida stanno qui: si regolano provando in gioco. L'{@code id} è anche il nome dell'entità, del geo ({@code geo/<id>.geo.json}) e della texture ({@code textures/entity/<id>.png}).
 */
public record VehicleDef(String id, int seats, double maxSpeed, double maxSteerDeg, double steerRateDeg,
        double wheelBase, double fuelCapacity, double fuelPer100Blocks, Powertrain powertrain) {

    public static final VehicleDef JEEP = new VehicleDef("jeep_wrangler", 2, 0.75, 30, 3, 2.75, 60, 0.08,
            Powertrain.JEEP);

    /** Dodge Charger '69 nera: muscle car bassa e potente, interasse 3,525 blocchi (assi a z -28,8 e +27,6). */
    public static final VehicleDef CHARGER_BLACK = new VehicleDef("dodge_charger_69_black", 2, 0.85, 28, 2.8,
            3.525, 70, 0.10, Powertrain.CHARGER);

    public static final List<VehicleDef> ALL = List.of(JEEP, CHARGER_BLACK);

    private static final Map<String, VehicleDef> OVERRIDES = new ConcurrentHashMap<>();

    /** Il veicolo con questo id, con i parametri configurati se ci sono. */
    public static Optional<VehicleDef> byId(String id) {
        VehicleDef configured = OVERRIDES.get(id);
        if (configured != null) return Optional.of(configured);
        return ALL.stream().filter(d -> d.id().equals(id)).findFirst();
    }

    /** Sostituisce i parametri di un veicolo (letti dalla configurazione). */
    public static void override(VehicleDef def) {
        OVERRIDES.put(def.id(), def);
    }

    public static void clearOverrides() {
        OVERRIDES.clear();
    }
}
