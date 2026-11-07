package dev.qrm.vehicles.mc;

import dev.qrm.vehicles.logic.VehicleHitboxes;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Le hitbox di ogni veicolo, lette una volta dal file del modello nel mod ({@code assets/qrm_vehicles/geo/<id>.geo.json}).
 * Servono sia al client (collisione, puntamento) sia al server (verifica dell'interazione), per questo si leggono dal
 * classpath e non dalle risorse del client. Un file mancante o rotto vale "nessuna hitbox": l'errore va nel log una volta.
 */
public final class VehicleHitboxFiles {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_vehicles");
    private static final Map<String, VehicleHitboxes> CACHE = new ConcurrentHashMap<>();

    private VehicleHitboxFiles() {}

    public static VehicleHitboxes of(String vehicleId) {
        return CACHE.computeIfAbsent(vehicleId, VehicleHitboxFiles::load);
    }

    private static VehicleHitboxes load(String vehicleId) {
        String path = "assets/qrm_vehicles/geo/" + vehicleId + ".geo.json";
        try (InputStream in = VehicleHitboxFiles.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return VehicleHitboxes.NONE;
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return VehicleHitboxes.parse(reader);
            }
        } catch (Exception e) {
            LOG.error("Hitbox del veicolo non lette: {} ({})", path, e.toString());
            return VehicleHitboxes.NONE;
        }
    }
}
