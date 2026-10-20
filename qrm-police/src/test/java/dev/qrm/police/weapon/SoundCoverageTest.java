package dev.qrm.police.weapon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Copertura dei suoni: ogni id usato dalle animazioni esiste in sounds.json e ogni .ogg ha una fonte dichiarata. */
class SoundCoverageTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/qrm_police");
    private static final Path ANIMATION = ASSETS.resolve("animations/police_pistol.json");
    private static final Path SOUNDS_JSON = ASSETS.resolve("sounds.json");
    private static final Path SOUNDS_DIR = ASSETS.resolve("sounds");
    private static final Path SOURCES = Path.of("AUDIO_SOURCES.md");

    private static final List<String> EXTRA_IDS = List.of(
            "pistol.slide_release", "pistol.slide_rack", "pistol.mag_inspect",
            "impact.metal", "impact.wood", "impact.stone", "impact.soft");

    private static Set<String> soundKeys() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(SOUNDS_JSON, StandardCharsets.UTF_8)).getAsJsonObject();
        return new TreeSet<>(root.keySet());
    }

    private static void collect(JsonElement e, Set<String> out) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("type") && "sound".equals(o.get("type").getAsString()) && o.has("args")) {
                out.add(o.getAsJsonArray("args").get(0).getAsString());
            }
            o.entrySet().forEach(en -> collect(en.getValue(), out));
        } else if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(x -> collect(x, out));
        }
    }

    @Test
    void everyAnimationSoundIdIsInSoundsJson() throws IOException {
        Set<String> used = new TreeSet<>();
        collect(JsonParser.parseString(Files.readString(ANIMATION, StandardCharsets.UTF_8)), used);
        assertTrue(!used.isEmpty(), "nessun evento sound trovato nell'animazione");
        Set<String> keys = soundKeys();
        List<String> missing = new ArrayList<>(used);
        missing.removeAll(keys);
        assertTrue(missing.isEmpty(), "suoni mancanti in sounds.json: " + missing);
    }

    @Test
    void everyOggIsListedInAudioSources() throws IOException {
        if (!Files.isDirectory(SOUNDS_DIR)) {
            return;
        }
        String sources = Files.readString(SOURCES, StandardCharsets.UTF_8);
        try (Stream<Path> files = Files.walk(SOUNDS_DIR)) {
            List<String> undocumented = files.filter(p -> p.toString().endsWith(".ogg"))
                    .map(p -> SOUNDS_DIR.relativize(p).toString().replace(File.separatorChar, '/'))
                    .filter(name -> !sources.contains(name))
                    .toList();
            assertTrue(undocumented.isEmpty(), "file .ogg senza fonte in AUDIO_SOURCES.md: " + undocumented);
        }
    }

    @Test
    void extraIdsExist() throws IOException {
        Set<String> keys = soundKeys();
        List<String> missing = new ArrayList<>(EXTRA_IDS);
        missing.removeAll(keys);
        assertTrue(missing.isEmpty(), "suoni extra mancanti: " + missing);
    }
}
