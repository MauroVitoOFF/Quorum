package dev.qrm.locker.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Contenuto di un armadietto in memoria: gli oggetti restano JSON (la conversione in {@code ItemStack} sta nel livello
 * Minecraft), più gli oggetti non leggibili, conservati tali e quali per quando tornerà il mod che li definiva.
 */
public final class LockerSlots {
    public static final int SIZE = 27;
    public static final int VERSION = 1;

    private final Map<Integer, JsonElement> items = new TreeMap<>();
    private final List<JsonObject> unreadable = new ArrayList<>();

    public static LockerSlots empty() {
        return new LockerSlots();
    }

    /** @param item serializzazione JSON dell'oggetto; sostituisce quello già nello slot */
    public void put(int slot, JsonElement item) {
        checkSlot(slot);
        items.put(slot, item);
    }

    public Map<Integer, JsonElement> items() {
        return Map.copyOf(items);
    }

    /** Aggiunge una voce che non si legge ({@code {"slot":n,"item":...}}); si riscrive senza modifiche. */
    public void addUnreadable(int slot, JsonElement item) {
        JsonObject entry = new JsonObject();
        entry.addProperty("slot", slot);
        entry.add("item", item);
        unreadable.add(entry);
    }

    public List<JsonObject> unreadable() {
        return List.copyOf(unreadable);
    }

    public String toJson() {
        JsonObject doc = new JsonObject();
        doc.addProperty("version", VERSION);
        JsonArray slots = new JsonArray();
        items.forEach((slot, item) -> slots.add(entry(slot, item)));
        doc.add("slots", slots);
        if (!unreadable.isEmpty()) {
            JsonArray bad = new JsonArray();
            unreadable.forEach(bad::add);
            doc.add("unreadable", bad);
        }
        return doc.toString();
    }

    /** @throws LockerDocumentException se il documento non è valido: in quel caso non va mai sovrascritto */
    public static LockerSlots fromJson(String json) throws LockerDocumentException {
        JsonObject doc;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) throw new LockerDocumentException("document is not an object");
            doc = root.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new LockerDocumentException("document is not valid JSON", e);
        }
        LockerSlots result = new LockerSlots();
        try {
            int version = doc.get("version").getAsInt();
            if (version != VERSION) throw new LockerDocumentException("unknown version " + version);
            for (JsonElement e : doc.getAsJsonArray("slots")) {
                JsonObject entry = e.getAsJsonObject();
                int slot = entry.get("slot").getAsInt();
                checkSlot(slot);
                if (result.items.put(slot, entry.get("item")) != null || entry.get("item") == null) {
                    throw new LockerDocumentException("duplicate or empty slot " + slot);
                }
            }
            if (doc.has("unreadable")) {
                for (JsonElement e : doc.getAsJsonArray("unreadable")) {
                    JsonObject entry = e.getAsJsonObject();
                    if (!entry.has("item")) throw new LockerDocumentException("unreadable entry without item");
                    result.unreadable.add(entry);
                }
            }
        } catch (LockerDocumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LockerDocumentException("malformed document", e);
        }
        return result;
    }

    private static JsonObject entry(int slot, JsonElement item) {
        JsonObject entry = new JsonObject();
        entry.addProperty("slot", slot);
        entry.add("item", item);
        return entry;
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SIZE) throw new IllegalArgumentException("slot out of range: " + slot);
    }
}
