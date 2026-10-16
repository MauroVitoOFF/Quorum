package dev.qrm.wardrobe.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import java.util.Optional;

/** La divisa indossata sta nei dati dei mod di QRM: namespace {@code qrm_wardrobe}, chiave {@code worn}. */
public final class ModDataWardrobeStorage implements WardrobeStorage {
    public static final String NAMESPACE = "qrm_wardrobe";
    public static final String KEY = "worn";

    private final ModDataService data;

    public ModDataWardrobeStorage(ModDataService data) {
        this.data = data;
    }

    @Override
    public Optional<String> read(CharacterId character) {
        Optional<String> raw = data.get(NAMESPACE, OwnerRef.character(character), KEY);
        if (raw.isEmpty()) return Optional.empty();
        try {
            JsonObject o = JsonParser.parseString(raw.get()).getAsJsonObject();
            return o.has("outfit") && o.get("outfit").isJsonPrimitive()
                    ? Optional.of(o.get("outfit").getAsString()).filter(s -> !s.isBlank())
                    : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public void write(CharacterId character, String outfitIdOrNull) {
        if (outfitIdOrNull == null) {
            data.delete(NAMESPACE, OwnerRef.character(character), KEY);
            return;
        }
        JsonObject o = new JsonObject();
        o.addProperty("outfit", outfitIdOrNull);
        data.put(NAMESPACE, OwnerRef.character(character), KEY, o.toString());
    }
}
