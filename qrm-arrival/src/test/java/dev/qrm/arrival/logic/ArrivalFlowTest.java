package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.arrival.logic.ArrivalSession.Phase;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArrivalFlowTest {
    static final Instant INSTALLED = Instant.parse("2026-11-03T10:00:00Z");
    static final ArrivalPoint ALLEY = new ArrivalPoint("minecraft:overworld", 1, 64, 2, 90f);

    static final class FakeStorage implements ArrivalStorage {
        final Set<CharacterId> arrived = new HashSet<>();

        @Override
        public boolean hasArrived(CharacterId c) {
            return arrived.contains(c);
        }

        @Override
        public void markArrived(CharacterId c) {
            arrived.add(c);
        }

        @Override
        public Instant installedAt() {
            return INSTALLED;
        }
    }

    static final class FakeHooks implements ArrivalFlow.Hooks {
        final List<String> log = new ArrayList<>();
        boolean teleportOk = true;

        @Override
        public boolean teleport(UUID player, ArrivalPoint point) {
            log.add("teleport");
            return teleportOk;
        }

        @Override
        public void giveKit(UUID player) {
            log.add("kit");
        }

        @Override
        public void phase(UUID player, Phase phase, int elapsed) {
            log.add("phase:" + phase);
        }

        long count(String s) {
            return log.stream().filter(s::equals).count();
        }
    }

    final FakeStorage storage = new FakeStorage();
    final FakeHooks hooks = new FakeHooks();
    final UUID player = UUID.randomUUID();
    Optional<ArrivalPoint> spawn = Optional.of(ALLEY);

    private ArrivalFlow flow(int lying, int rising, int max) {
        ScenarioPicker picker = new ScenarioPicker(List.of(new AlleyScenario(() -> spawn)), new Random(1));
        return new ArrivalFlow(new ArrivalPolicy(storage), storage, picker,
                new ArrivalFiles.Timing(lying, rising, max), hooks);
    }

    private static PlayerCharacter fresh() {
        return new PlayerCharacter(new CharacterId(UUID.randomUUID()), new PlayerId(UUID.randomUUID()), "Anna", "Rossi",
                LocalDate.of(1990, 1, 1), CharacterStatus.ACTIVE, INSTALLED.plusSeconds(60));
    }

    private static void tick(ArrivalFlow f, UUID p, int n) {
        for (int i = 0; i < n; i++) f.tick(p);
    }

    @Test
    void newCharacter_isTeleportedAndLocked() {
        ArrivalFlow f = flow(3, 2, 100);
        assertTrue(f.onSelected(player, fresh()));
        assertEquals(1, hooks.count("teleport"));
        assertTrue(f.locked(player));
    }

    @Test
    void secondSelectionOfTheSameCharacter_startsOnlyOnce() {
        ArrivalFlow f = flow(3, 2, 100);
        PlayerCharacter c = fresh();
        assertTrue(f.onSelected(player, c));
        assertFalse(f.onSelected(player, c));
        assertEquals(1, hooks.count("teleport"));
    }

    @Test
    void completion_givesKitAndMarks_once() {
        ArrivalFlow f = flow(3, 2, 100);
        PlayerCharacter c = fresh();
        f.onSelected(player, c);
        tick(f, player, 5);
        assertEquals(1, hooks.count("kit"));
        assertTrue(storage.hasArrived(c.id()));
        assertFalse(f.locked(player));
        tick(f, player, 10);
        assertEquals(1, hooks.count("kit"));
        assertFalse(f.onSelected(player, c));
    }

    @Test
    void switchingCharacterMidAnimation_unlocksAndFirstIsNotMarked() {
        ArrivalFlow f = flow(3, 2, 100);
        PlayerCharacter first = fresh();
        PlayerCharacter second = fresh();
        f.onSelected(player, first);
        tick(f, player, 2);
        assertTrue(f.onSelected(player, second));
        tick(f, player, 5);
        assertFalse(storage.hasArrived(first.id()));
        assertTrue(storage.hasArrived(second.id()));
        assertEquals(1, hooks.count("kit"));
    }

    @Test
    void disconnectMidAnimation_noKitNoMark_thenReselectRestarts() {
        ArrivalFlow f = flow(3, 2, 100);
        PlayerCharacter c = fresh();
        f.onSelected(player, c);
        tick(f, player, 2);
        f.forget(player);
        assertFalse(f.locked(player));
        assertEquals(0, hooks.count("kit"));
        assertFalse(storage.hasArrived(c.id()));
        assertTrue(f.onSelected(player, c));
        assertEquals(2, hooks.count("teleport"));
    }

    @Test
    void noArrivalPointSet_doesNothing() {
        spawn = Optional.empty();
        ArrivalFlow f = flow(3, 2, 100);
        assertFalse(f.onSelected(player, fresh()));
        assertFalse(f.locked(player));
        assertEquals(0, hooks.count("teleport"));
    }

    @Test
    void teleportFailing_startsNothing_andCharacterIsNotMarked() {
        hooks.teleportOk = false;
        ArrivalFlow f = flow(3, 2, 100);
        PlayerCharacter c = fresh();
        assertFalse(f.onSelected(player, c));
        assertFalse(f.locked(player));
        assertFalse(storage.hasArrived(c.id()));
        assertEquals(0, hooks.count("phase:LYING"));
    }

    @Test
    void characterThatShouldNotArrive_doesNothing() {
        ArrivalFlow f = flow(3, 2, 100);
        PlayerCharacter old = new PlayerCharacter(new CharacterId(UUID.randomUUID()), new PlayerId(UUID.randomUUID()),
                "Luca", "Bianchi", LocalDate.of(1980, 1, 1), CharacterStatus.ACTIVE, INSTALLED.minusSeconds(60));
        assertFalse(f.onSelected(player, old));
        assertEquals(0, hooks.count("teleport"));
    }

    @Test
    void lockExpiryBeforeCompletion_unlocksWithoutKitOrMark() {
        ArrivalFlow f = flow(50, 50, 10);
        PlayerCharacter c = fresh();
        f.onSelected(player, c);
        tick(f, player, 10);
        assertFalse(f.locked(player));
        assertEquals(0, hooks.count("kit"));
        assertFalse(storage.hasArrived(c.id()));
    }
}
