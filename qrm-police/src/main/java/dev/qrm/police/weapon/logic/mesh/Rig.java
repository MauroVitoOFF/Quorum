package dev.qrm.police.weapon.logic.mesh;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Lo scheletro della pistola letto da {@code rigs/police_pistol.json}: bone con genitore, pivot (unità del modello,
 * 16 = 1 blocco, coordinate assolute) e se ha una mesh propria. I bone sono in ordine genitore-prima-del-figlio,
 * così chi disegna può accumulare le trasformazioni scorrendo la lista una volta.
 */
public record Rig(List<Bone> bones, @Nullable String partsPrefix) {

    public static final Rig EMPTY = new Rig(List.of(), null);

    public Rig {
        bones = List.copyOf(bones);
    }

    /** Un bone: {@code parent} null per la radice. */
    public record Bone(String name, @Nullable String parent, float[] pivot, boolean hasModel) {}

    public Optional<Bone> bone(String name) {
        for (Bone b : bones) if (b.name().equals(name)) return Optional.of(b);
        return Optional.empty();
    }

    /** Legge il rig. Genitore inesistente, ciclo o pivot mancante: IllegalArgumentException. */
    public static Rig parse(Reader json) {
        JsonObject root;
        try {
            root = JsonParser.parseReader(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Rig non leggibile: " + e.getMessage(), e);
        }
        if (!root.has("bones")) throw new IllegalArgumentException("Rig senza 'bones'");
        Map<String, Bone> byName = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("bones").entrySet()) {
            JsonObject b = e.getValue().getAsJsonObject();
            JsonElement p = b.get("parent");
            String parent = p == null || p.isJsonNull() ? null : p.getAsString();
            if (!b.has("pivot")) throw new IllegalArgumentException("Bone '" + e.getKey() + "' senza pivot");
            JsonArray pv = b.getAsJsonArray("pivot");
            if (pv.size() != 3) throw new IllegalArgumentException("Pivot di '" + e.getKey() + "' non ha 3 valori");
            float[] pivot = {pv.get(0).getAsFloat(), pv.get(1).getAsFloat(), pv.get(2).getAsFloat()};
            boolean hasModel = b.has("has_model") && b.get("has_model").getAsBoolean();
            byName.put(e.getKey(), new Bone(e.getKey(), parent, pivot, hasModel));
        }
        for (Bone b : byName.values()) {
            if (b.parent() != null && !byName.containsKey(b.parent())) {
                throw new IllegalArgumentException("Bone '" + b.name() + "': genitore sconosciuto '" + b.parent() + "'");
            }
        }
        // Ordinamento: si aggiunge un bone quando il suo genitore è già in lista; se un giro non aggiunge nulla
        // restano solo bone in un ciclo.
        List<Bone> ordered = new ArrayList<>(byName.size());
        List<String> placed = new ArrayList<>();
        while (ordered.size() < byName.size()) {
            boolean progress = false;
            for (Bone b : byName.values()) {
                if (placed.contains(b.name())) continue;
                if (b.parent() == null || placed.contains(b.parent())) {
                    ordered.add(b);
                    placed.add(b.name());
                    progress = true;
                }
            }
            if (!progress) throw new IllegalArgumentException("Ciclo tra i bone del rig");
        }
        JsonElement prefix = root.get("parts_prefix");
        return new Rig(ordered, prefix == null || prefix.isJsonNull() ? null : prefix.getAsString());
    }
}
