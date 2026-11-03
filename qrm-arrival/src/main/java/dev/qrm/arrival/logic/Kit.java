package dev.qrm.arrival.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/** Ciò che il personaggio ha addosso al risveglio, da {@code config/qrm_arrival/kit.json}. */
public final class Kit {
    /** 10000 unità minime sono 100,00 nella valuta di esempio {@code QRM} (2 decimali). */
    public static final String DEFAULT_JSON = """
            {
              "items": [
                { "item": "qrm_bank:cash_qrm_10000", "count": 1 }
              ]
            }
            """;

    private final List<KitEntry> entries;

    private Kit(List<KitEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static Kit defaults() {
        return parse(DEFAULT_JSON);
    }

    /** @throws IllegalArgumentException se il JSON è rotto o una voce non è valida */
    public static Kit parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            if (items == null) throw new IllegalArgumentException("missing \"items\"");
            List<KitEntry> out = new ArrayList<>();
            for (JsonElement e : items) {
                JsonObject o = e.getAsJsonObject();
                out.add(new KitEntry(o.get("item").getAsString(), o.get("count").getAsInt()));
            }
            return new Kit(out);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid kit: " + e.getMessage(), e);
        }
    }

    public List<KitEntry> entries() {
        return entries;
    }
}
