package dev.qrm.arrival.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * I file del Module sul server, in una cartella (di solito {@code config/qrm_arrival/}): {@code kit.json},
 * {@code spawn.json} e {@code timing.json}. Un file rotto non blocca l'arrivo: si usano i valori predefiniti e il
 * problema va a chi ascolta {@code problems} (il log).
 */
public final class ArrivalFiles {
    /** Durate in tick: a terra, per rialzarsi, e blocco massimo oltre il quale il giocatore viene comunque liberato. */
    public record Timing(int lyingTicks, int risingTicks, int maxLockTicks) {
        public static final Timing DEFAULTS = new Timing(60, 40, 400);
    }

    private final Path dir;
    private final Consumer<String> problems;
    private Kit kit = Kit.defaults();
    private Optional<ArrivalPoint> spawn = Optional.empty();
    private Timing timing = Timing.DEFAULTS;

    public ArrivalFiles(Path dir, Consumer<String> problems) {
        this.dir = dir;
        this.problems = problems;
        reload();
    }

    public void reload() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            problems.accept("impossibile creare " + dir + ": " + e.getMessage());
        }
        kit = readKit();
        spawn = readSpawn();
        timing = readTiming();
    }

    public Kit kit() {
        return kit;
    }

    public Optional<ArrivalPoint> spawn() {
        return spawn;
    }

    public Timing timing() {
        return timing;
    }

    public void saveSpawn(ArrivalPoint point) {
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("spawn.json"), point.toJson());
            spawn = Optional.of(point);
        } catch (IOException e) {
            problems.accept("impossibile scrivere spawn.json: " + e.getMessage());
        }
    }

    private Kit readKit() {
        Path file = dir.resolve("kit.json");
        try {
            if (!Files.exists(file)) Files.writeString(file, Kit.DEFAULT_JSON);
            return Kit.parse(Files.readString(file));
        } catch (IOException | IllegalArgumentException e) {
            problems.accept("kit.json non valido, uso il kit predefinito: " + e.getMessage());
            return Kit.defaults();
        }
    }

    private Optional<ArrivalPoint> readSpawn() {
        Path file = dir.resolve("spawn.json");
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(ArrivalPoint.parse(Files.readString(file)));
        } catch (IOException | IllegalArgumentException e) {
            problems.accept("spawn.json non valido, il punto di arrivo risulta non impostato: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Timing readTiming() {
        Path file = dir.resolve("timing.json");
        if (!Files.exists(file)) return Timing.DEFAULTS;
        try {
            JsonObject o = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return new Timing(positive(o, "lyingTicks", Timing.DEFAULTS.lyingTicks()),
                    positive(o, "risingTicks", Timing.DEFAULTS.risingTicks()),
                    positive(o, "maxLockTicks", Timing.DEFAULTS.maxLockTicks()));
        } catch (IOException | RuntimeException e) {
            problems.accept("timing.json non valido, uso le durate predefinite: " + e.getMessage());
            return Timing.DEFAULTS;
        }
    }

    private static int positive(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return fallback;
        int v = e.getAsInt();
        return v > 0 ? v : fallback;
    }
}
