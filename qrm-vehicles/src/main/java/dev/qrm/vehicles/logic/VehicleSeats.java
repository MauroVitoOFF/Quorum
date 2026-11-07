package dev.qrm.vehicles.logic;

import dev.qrm.client.api.rig.GeoModel;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Posizione dei sedili di ogni modello, nelle unità del modello (16 = 1 blocco) e già nello spazio di gioco. I sedili
 * si leggono dai locator {@code driver_hip} e {@code passenger_hip} del file del modello
 * ({@code assets/qrm_vehicles/geo/<id>.geo.json}): per spostarli basta muovere il locator in Blockbench ed esportare.
 * Le costanti qui sotto sono il ripiego per un modello senza locator, e i test verificano che coincidano con i file.
 * L'indice 0 è il guidatore.
 */
public final class VehicleSeats {

    private VehicleSeats() {}

    public static final List<float[]> JEEP = List.of(new float[] {-8f, 8.4f, 3.75f}, new float[] {8f, 8.4f, 3.75f});

    /** Sedili bassi della Charger (cuscino y 9,6): l'anca sta 4,2 unità sotto la superficie perché il tetto è a y 27,8. */
    public static final List<float[]> CHARGER_BLACK = List.of(new float[] {-7.2f, 5.4f, 1.8f}, new float[] {7.2f, 5.4f, 1.8f});

    private static final Map<String, List<float[]>> CACHE = new ConcurrentHashMap<>();

    public static List<float[]> of(VehicleDef def) {
        return CACHE.computeIfAbsent(def.id(), id -> fromModel(id).orElseGet(() -> fallback(def)));
    }

    private static List<float[]> fallback(VehicleDef def) {
        return def.id().equals(VehicleDef.CHARGER_BLACK.id()) ? CHARGER_BLACK : JEEP;
    }

    /** I sedili dai locator del modello, se ci sono tutti e due. */
    public static Optional<List<float[]>> fromLocators(Map<String, float[]> locators) {
        float[] driver = locators.get("driver_hip");
        float[] passenger = locators.get("passenger_hip");
        if (driver == null || passenger == null) return Optional.empty();
        return Optional.of(List.of(driver.clone(), passenger.clone()));
    }

    private static Optional<List<float[]>> fromModel(String vehicleId) {
        String path = "assets/qrm_vehicles/geo/" + vehicleId + ".geo.json";
        try (InputStream in = VehicleSeats.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return Optional.empty();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return fromLocators(GeoModel.parse(reader).locators());
            }
        } catch (Exception e) {
            // Un modello illeggibile vale "nessun locator": si usano le costanti.
            return Optional.empty();
        }
    }
}
