package dev.qrm.police.weapon.logic.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lettura del formato {@code qrm.anim/2} (JSON esportato da tools/pistol_export.py). */
public final class AnimFormat {

    public static final String FORMAT = "qrm.anim/2";

    private AnimFormat() {}

    /** Legge tutte le clip del file, per nome. Formato diverso da {@link #FORMAT}: IllegalArgumentException. */
    public static Map<String, AnimClip> parse(Reader json) {
        JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
        String format = root.has("format") ? root.get("format").getAsString() : null;
        if (!FORMAT.equals(format)) {
            throw new IllegalArgumentException("Formato di animazione non supportato: " + format);
        }
        Map<String, AnimClip> clips = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("animations").entrySet()) {
            clips.put(e.getKey(), clip(e.getValue().getAsJsonObject()));
        }
        return clips;
    }

    private static AnimClip clip(JsonObject o) {
        Map<String, AnimClip.Bone> bones = new LinkedHashMap<>();
        if (o.has("bones")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("bones").entrySet()) {
                JsonObject b = e.getValue().getAsJsonObject();
                bones.put(e.getKey(), new AnimClip.Bone(channel(b, "position"), channel(b, "rotation"), channel(b, "scale")));
            }
        }
        List<AnimEvent> events = new ArrayList<>();
        if (o.has("events")) {
            for (JsonElement el : o.getAsJsonArray("events")) {
                JsonObject ev = el.getAsJsonObject();
                List<String> args = new ArrayList<>();
                if (ev.has("args")) for (JsonElement a : ev.getAsJsonArray("args")) args.add(a.getAsString());
                events.add(new AnimEvent(ev.get("t").getAsDouble(), ev.get("type").getAsString(), args));
            }
        }
        return new AnimClip(o.get("length").getAsDouble(),
                o.has("loop") && o.get("loop").getAsBoolean(),
                o.has("hold_last") && o.get("hold_last").getAsBoolean(), bones, events);
    }

    private static AnimChannel channel(JsonObject bone, String name) {
        if (!bone.has(name)) return null;
        List<AnimKey> keys = new ArrayList<>();
        for (JsonElement el : bone.getAsJsonArray(name)) {
            JsonObject k = el.getAsJsonObject();
            JsonArray v = k.getAsJsonArray("v");
            float[] val = {v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat()};
            String interp = k.has("interp") ? k.get("interp").getAsString() : "linear";
            keys.add(new AnimKey(k.get("t").getAsDouble(), val, AnimKey.Interp.parse(interp)));
        }
        keys.sort((a, b) -> Double.compare(a.t(), b.t()));
        return new AnimChannel(keys);
    }
}
