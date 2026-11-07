package dev.qrm.vehicles.logic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Il fondo su cui poggiano le ruote. {@code gripFactor} moltiplica l'attrito delle gomme, {@code speedFactor} la
 * velocità di punta consentita, {@code rollFactor} la resistenza al rotolamento (1 = asfalto). I valori predefiniti si
 * possono sostituire da {@code config/qrm_vehicles/surfaces.json}; quali blocchi appartengono a un fondo lo decidono i
 * tag {@code qrm_vehicles:surface/<id>}, modificabili con un datapack.
 */
public record Surface(String id, double gripFactor, double speedFactor, double rollFactor) {

    /** Pietra, cemento, terracotta, asfalto e ogni blocco non elencato: presa piena e un po' meno resistenza. */
    public static final Surface PAVED = new Surface("paved", 1.0, 1.0, 0.9);
    /** Terra ed erba: buona presa ma non perfetta, si deraglia più facilmente. */
    public static final Surface DIRT = new Surface("dirt", 0.82, 0.9, 2.0);
    /**
     * Sabbia, ghiaia e fango: l'auto affonda (molta resistenza, punta a meno di metà) e la presa laterale è scarsa.
     * Con 0,65 di punta il Charger restava a 84 km/h e la differenza non si sentiva.
     */
    public static final Surface SAND = new Surface("sand", 0.6, 0.4, 8.0);
    /** Neve: presa bassissima in trazione e frenata, un po' di resistenza. */
    public static final Surface SNOW = new Surface("snow", 0.35, 0.8, 2.5);
    /** Ghiaccio: l'auto scivola per inerzia, quasi senza resistenza al rotolamento. */
    public static final Surface ICE = new Surface("ice", 0.15, 1.0, 0.8);

    private static final List<Surface> DEFAULTS = List.of(PAVED, DIRT, SAND, SNOW, ICE);
    private static final Map<String, Surface> TABLE = new ConcurrentHashMap<>();

    public Surface {
        if (!(gripFactor > 0) || !(speedFactor > 0) || !(rollFactor > 0)) {
            throw new IllegalArgumentException("fondo " + id + ": grip, speed e roll devono essere positivi");
        }
    }

    /** I fondi predefiniti. */
    public static Surface[] values() {
        return DEFAULTS.toArray(new Surface[0]);
    }

    /** Il fondo con questo id, con gli eventuali valori configurati al posto di quelli predefiniti. */
    public static Surface get(String id) {
        Surface configured = TABLE.get(id);
        if (configured != null) return configured;
        return DEFAULTS.stream().filter(s -> s.id().equals(id)).findFirst().orElse(PAVED);
    }

    /** Sostituisce i valori di un fondo (letti dalla configurazione). */
    public static void override(Surface surface) {
        TABLE.put(surface.id(), surface);
    }

    public static void clearOverrides() {
        TABLE.clear();
    }

    /** Lo stesso fondo con i valori attuali della configurazione (le costanti restano quelle predefinite). */
    public Surface current() {
        return get(id);
    }
}
