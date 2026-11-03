package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArrivalFilesTest {
    @TempDir
    Path dir;

    final List<String> problems = new ArrayList<>();

    private ArrivalFiles files() {
        return new ArrivalFiles(dir, problems::add);
    }

    @Test
    void missingKitFile_isCreatedWithDefault() {
        ArrivalFiles f = files();
        assertEquals(Kit.defaults().entries(), f.kit().entries());
        assertTrue(Files.exists(dir.resolve("kit.json")));
        assertTrue(problems.isEmpty());
    }

    @Test
    void brokenKitFile_fallsBackToDefaultKit() throws IOException {
        Files.writeString(dir.resolve("kit.json"), "{broken");
        ArrivalFiles f = files();
        assertEquals(Kit.defaults().entries(), f.kit().entries());
        assertEquals(1, problems.size());
    }

    @Test
    void customKitFile_isRead() throws IOException {
        Files.writeString(dir.resolve("kit.json"), "{\"items\":[{\"item\":\"minecraft:bread\",\"count\":2}]}");
        assertEquals(List.of(new KitEntry("minecraft:bread", 2)), files().kit().entries());
    }

    @Test
    void spawn_roundTripsThroughFile() {
        ArrivalFiles f = files();
        assertEquals(Optional.empty(), f.spawn());
        ArrivalPoint p = new ArrivalPoint("minecraft:overworld", 5, 70, 9, 45f);
        f.saveSpawn(p);
        assertEquals(Optional.of(p), files().spawn());
    }

    @Test
    void brokenSpawnFile_isTreatedAsNotSet() throws IOException {
        Files.writeString(dir.resolve("spawn.json"), "nope");
        assertEquals(Optional.empty(), files().spawn());
        assertEquals(1, problems.size());
    }

    @Test
    void timingFile_missingKeysUseDefaults() throws IOException {
        Files.writeString(dir.resolve("timing.json"), "{\"lyingTicks\":20}");
        ArrivalFiles.Timing t = files().timing();
        assertEquals(20, t.lyingTicks());
        assertEquals(ArrivalFiles.Timing.DEFAULTS.risingTicks(), t.risingTicks());
        assertEquals(ArrivalFiles.Timing.DEFAULTS.maxLockTicks(), t.maxLockTicks());
    }

    @Test
    void timingFile_nonPositiveValuesUseDefaults() throws IOException {
        Files.writeString(dir.resolve("timing.json"), "{\"lyingTicks\":0,\"risingTicks\":-5}");
        assertEquals(ArrivalFiles.Timing.DEFAULTS, files().timing());
    }

    @Test
    void timingFile_lockLimitShorterThanTheAnimation_isRaisedAndReported() throws IOException {
        Files.writeString(dir.resolve("timing.json"), "{\"lyingTicks\":500}");
        ArrivalFiles.Timing t = files().timing();
        assertEquals(500, t.lyingTicks());
        assertEquals(t.lyingTicks() + t.risingTicks(), t.maxLockTicks());
        assertEquals(1, problems.size());
    }

    @Test
    void reload_picksUpChangedFiles() throws IOException {
        ArrivalFiles f = files();
        Files.writeString(dir.resolve("kit.json"), "{\"items\":[]}");
        f.reload();
        assertTrue(f.kit().entries().isEmpty());
    }
}
