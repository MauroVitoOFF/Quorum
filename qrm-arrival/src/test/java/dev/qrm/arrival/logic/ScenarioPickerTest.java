package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScenarioPickerTest {
    record Fake(String id, Optional<ArrivalPoint> point) implements Scenario {}

    static final ArrivalPoint P = new ArrivalPoint("minecraft:overworld", 1, 64, 2, 90f);

    @Test
    void pick_withOneUsableScenario_returnsIt() {
        Fake alley = new Fake("alley", Optional.of(P));
        assertEquals(Optional.of(alley), new ScenarioPicker(List.of(alley), new Random(1)).pick());
    }

    @Test
    void pick_skipsScenarioWithoutPoint() {
        Fake empty = new Fake("empty", Optional.empty());
        Fake alley = new Fake("alley", Optional.of(P));
        for (int i = 0; i < 20; i++) {
            assertEquals(Optional.of(alley), new ScenarioPicker(List.of(empty, alley), new Random(i)).pick());
        }
    }

    @Test
    void pick_noUsable_returnsEmpty() {
        assertEquals(Optional.empty(), new ScenarioPicker(List.of(new Fake("empty", Optional.empty())), new Random(1)).pick());
        assertEquals(Optional.empty(), new ScenarioPicker(List.of(), new Random(1)).pick());
    }

    @Test
    void pick_withTwoUsable_returnsBothOverManyDraws() {
        ScenarioPicker picker = new ScenarioPicker(
                List.of(new Fake("a", Optional.of(P)), new Fake("b", Optional.of(P))), new Random(42));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) seen.add(picker.pick().orElseThrow().id());
        assertEquals(Set.of("a", "b"), seen);
    }
}
