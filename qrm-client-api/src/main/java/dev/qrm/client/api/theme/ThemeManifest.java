package dev.qrm.client.api.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Manifesto del tema (colori, misure, texture). Puro: nessuna dipendenza da Minecraft.
 * Un pack può sovrascriverlo parzialmente: le chiavi mancanti o invalide ricadono sul predefinito.
 */
public final class ThemeManifest {
    public static final int VERSION = 1;
    private static final Pattern COLOR = Pattern.compile("#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})");
    private static final Pattern TEXTURE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final int MAX_TEXTURE_SIZE = 4096;

    public record Slice(int left, int top, int right, int bottom) {}

    public record Texture(String id, int width, int height, Optional<Slice> slice) {}

    public record Result(ThemeManifest manifest, List<String> warnings, boolean usedFallback) {}

    private final Map<String, Integer> colors;
    private final Map<String, Double> metrics;
    private final Map<String, Texture> textures;

    private ThemeManifest(Map<String, Integer> colors, Map<String, Double> metrics, Map<String, Texture> textures) {
        this.colors = Collections.unmodifiableMap(colors);
        this.metrics = Collections.unmodifiableMap(metrics);
        this.textures = Collections.unmodifiableMap(textures);
    }

    public int color(String key) {
        Integer v = colors.get(key);
        if (v == null) throw new IllegalArgumentException("unknown theme color: " + key);
        return v;
    }

    public double metric(String key) {
        Double v = metrics.get(key);
        if (v == null) throw new IllegalArgumentException("unknown theme metric: " + key);
        return v;
    }

    public Texture texture(String key) {
        Texture t = textures.get(key);
        if (t == null) throw new IllegalArgumentException("unknown theme texture: " + key);
        return t;
    }

    public Set<String> textureKeys() {
        return textures.keySet();
    }

    /**
     * Aggiunge le voci di un modulo. Le chiavi devono iniziare per {@code modId + "."}: quelle fuori prefisso o già
     * esistenti sono scartate con un avviso. Il manifesto corrente non cambia.
     */
    public ThemeManifest withFragment(String modId, ThemeManifest fragment, List<String> warnings) {
        String prefix = modId + ".";
        Map<String, Integer> c = new LinkedHashMap<>(colors);
        Map<String, Double> m = new LinkedHashMap<>(metrics);
        Map<String, Texture> t = new LinkedHashMap<>(textures);
        mergeInto(c, fragment.colors, prefix, "color", warnings);
        mergeInto(m, fragment.metrics, prefix, "metric", warnings);
        mergeInto(t, fragment.textures, prefix, "texture", warnings);
        return new ThemeManifest(c, m, t);
    }

    private static <T> void mergeInto(Map<String, T> target, Map<String, T> source, String prefix, String kind,
            List<String> warnings) {
        for (Map.Entry<String, T> e : source.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith(prefix)) {
                warnings.add(kind + " '" + key + "' ignored: keys of this module must start with '" + prefix + "'");
            } else if (target.containsKey(key)) {
                warnings.add(kind + " '" + key + "' ignored: already defined");
            } else {
                target.put(key, e.getValue());
            }
        }
    }

    /**
     * Legge il manifesto predefinito di chi possiede le risorse (QRM o un modulo): modalità rigida, una voce non valida è
     * un errore. Non legge nulla dal classpath: il testo lo fornisce il chiamante.
     */
    public static ThemeManifest parseBuiltIn(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return build(root, null, new ArrayList<>());
        } catch (RuntimeException e) {
            throw new IllegalStateException("Built-in theme is invalid: " + e.getMessage(), e);
        }
    }

    /** Legge un manifesto (di un pack): le parti invalide ricadono sui predefiniti; JSON rotto o versione ignota = tutto il predefinito. */
    public static Result parse(String json, ThemeManifest defaults) {
        List<String> warnings = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) throw new JsonParseException("not an object");
            root = el.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            warnings.add("invalid theme json: " + e.getMessage());
            return new Result(defaults, warnings, true);
        }
        JsonElement version = root.get("version");
        if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                || version.getAsInt() != VERSION) {
            warnings.add("unsupported theme version (expected " + VERSION + ")");
            return new Result(defaults, warnings, true);
        }
        return new Result(build(root, defaults, warnings), warnings, false);
    }

    /** {@code defaults == null}: manifesto predefinito, qualsiasi voce invalida è un errore di build. */
    private static ThemeManifest build(JsonObject root, ThemeManifest defaults, List<String> warnings) {
        JsonObject jc = section(root, "colors", warnings);
        JsonObject jm = section(root, "metrics", warnings);
        JsonObject jt = section(root, "textures", warnings);
        Map<String, Integer> colors = new LinkedHashMap<>();
        Map<String, Double> metrics = new LinkedHashMap<>();
        Map<String, Texture> textures = new LinkedHashMap<>();

        for (String key : defaults != null ? defaults.colors.keySet() : jc.keySet()) {
            Integer v = jc.has(key) ? parseColor(jc.get(key)) : null;
            if (jc.has(key) && v == null) warnings.add("invalid color '" + key + "', using the default");
            colors.put(key, resolve(v, defaults == null ? null : defaults.colors.get(key), "color " + key));
        }
        for (String key : defaults != null ? defaults.metrics.keySet() : jm.keySet()) {
            Double v = jm.has(key) ? parseMetric(jm.get(key)) : null;
            if (jm.has(key) && v == null) warnings.add("invalid metric '" + key + "', using the default");
            metrics.put(key, resolve(v, defaults == null ? null : defaults.metrics.get(key), "metric " + key));
        }
        for (String key : defaults != null ? defaults.textures.keySet() : jt.keySet()) {
            Texture v = jt.has(key) ? parseTexture(jt.get(key)) : null;
            if (jt.has(key) && v == null) warnings.add("invalid texture '" + key + "', using the default");
            textures.put(key, resolve(v, defaults == null ? null : defaults.textures.get(key), "texture " + key));
        }
        return new ThemeManifest(colors, metrics, textures);
    }

    private static <T> T resolve(T parsed, T fallback, String what) {
        if (parsed != null) return parsed;
        if (fallback != null) return fallback;
        throw new IllegalStateException("invalid " + what);
    }

    private static JsonObject section(JsonObject root, String name, List<String> warnings) {
        JsonElement e = root.get(name);
        if (e == null) return new JsonObject();
        if (!e.isJsonObject()) {
            warnings.add("'" + name + "' must be an object");
            return new JsonObject();
        }
        return e.getAsJsonObject();
    }

    private static Integer parseColor(JsonElement e) {
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) return null;
        var m = COLOR.matcher(e.getAsString());
        if (!m.matches()) return null;
        String hex = m.group(1);
        long value = Long.parseLong(hex, 16);
        return hex.length() == 6 ? (int) (0xFF000000L | value) : (int) value;
    }

    private static Double parseMetric(JsonElement e) {
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        double v = e.getAsDouble();
        return v > 0 && Double.isFinite(v) ? v : null;
    }

    private static Texture parseTexture(JsonElement e) {
        if (!e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        JsonElement id = o.get("id");
        if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()
                || !TEXTURE_ID.matcher(id.getAsString()).matches()) {
            return null;
        }
        int[] size = ints(o.get("size"), 2);
        if (size == null || size[0] <= 0 || size[1] <= 0 || size[0] > MAX_TEXTURE_SIZE || size[1] > MAX_TEXTURE_SIZE) {
            return null;
        }
        Optional<Slice> slice = Optional.empty();
        if (o.has("slice")) {
            int[] s = ints(o.get("slice"), 4);
            if (s == null || s[0] < 0 || s[1] < 0 || s[2] < 0 || s[3] < 0 || (long) s[0] + s[2] >= size[0] || (long) s[1] + s[3] >= size[1]) {
                return null;
            }
            slice = Optional.of(new Slice(s[0], s[1], s[2], s[3]));
        }
        return new Texture(id.getAsString(), size[0], size[1], slice);
    }

    private static int[] ints(JsonElement e, int count) {
        if (e == null || !e.isJsonArray()) return null;
        JsonArray a = e.getAsJsonArray();
        if (a.size() != count) return null;
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            JsonElement x = a.get(i);
            if (!x.isJsonPrimitive() || !x.getAsJsonPrimitive().isNumber()) return null;
            double d = x.getAsDouble();
            if (!Double.isFinite(d) || Math.abs(d) > 100000 || d != Math.rint(d)) return null;
            out[i] = (int) d;
        }
        return out;
    }
}
