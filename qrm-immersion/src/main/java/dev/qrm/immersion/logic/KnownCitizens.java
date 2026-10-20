package dev.qrm.immersion.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chi un Character ha incontrato: UUID del bersaglio → nome RP al momento dell'ultima presentazione
 * (il nome si aggiorna quando chi è conosciuto è online, vedi ImmersionServer).
 */
public final class KnownCitizens {
    private final Map<UUID, String> known = new LinkedHashMap<>();

    public boolean knows(UUID id) {
        return known.containsKey(id);
    }

    /** @return vero se è una conoscenza nuova o il nome è cambiato. */
    public boolean learn(UUID id, String name) {
        String previous = known.put(id, name);
        return !name.equals(previous);
    }

    public Map<UUID, String> view() {
        return Collections.unmodifiableMap(known);
    }

    public boolean isEmpty() {
        return known.isEmpty();
    }

    public String toJson() {
        JsonObject o = new JsonObject();
        known.forEach((id, name) -> o.addProperty(id.toString(), name));
        return o.toString();
    }

    /** Un testo illeggibile dà una lista vuota: meglio ripresentarsi che perdere il personaggio. */
    public static KnownCitizens fromJson(String json) {
        KnownCitizens k = new KnownCitizens();
        try {
            for (var e : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
                k.known.put(UUID.fromString(e.getKey()), e.getValue().getAsString());
            }
        } catch (RuntimeException ignored) {
            k.known.clear();
        }
        return k;
    }
}
