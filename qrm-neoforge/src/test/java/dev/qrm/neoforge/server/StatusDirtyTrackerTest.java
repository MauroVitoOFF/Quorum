package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StatusDirtyTrackerTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID charA = UUID.randomUUID();

    @Test
    void repeatedMarksGiveOneEntryAndDrainEmptiesTheSet() {
        StatusDirtyTracker t = new StatusDirtyTracker();
        t.markPlayer(a);
        t.markPlayer(a);
        t.markPlayer(b);
        assertEquals(Set.of(a, b), t.drain());
        assertEquals(Set.of(), t.drain());
    }

    @Test
    void markAllMarksEveryPlayer() {
        StatusDirtyTracker t = new StatusDirtyTracker();
        t.markAll(List.of(a, b));
        assertEquals(Set.of(a, b), t.drain());
    }

    @Test
    void markCharacterMarksThePlayerWhoHasItActive() {
        StatusDirtyTracker t = new StatusDirtyTracker();
        t.setActiveCharacter(a, charA);
        t.markCharacter(charA);
        assertEquals(Set.of(a), t.drain());
    }

    @Test
    void markCharacterIsIgnoredWhenNobodyHasItActive() {
        StatusDirtyTracker t = new StatusDirtyTracker();
        t.markCharacter(charA);
        assertEquals(Set.of(), t.drain());
    }

    @Test
    void settingANullCharacterClearsTheMapping() {
        StatusDirtyTracker t = new StatusDirtyTracker();
        t.setActiveCharacter(a, charA);
        t.setActiveCharacter(a, null);
        t.markCharacter(charA);
        assertEquals(Set.of(), t.drain());
    }

    @Test
    void forgetRemovesPendingMarksAndTheMapping() {
        StatusDirtyTracker t = new StatusDirtyTracker();
        t.setActiveCharacter(a, charA);
        t.markPlayer(a);
        t.forget(a);
        t.markCharacter(charA);
        assertEquals(Set.of(), t.drain());
    }

    @Test
    void concurrentMarksAreAllDrainedExactlyOnce() throws Exception {
        StatusDirtyTracker t = new StatusDirtyTracker();
        int threads = 8;
        int perThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        Set<UUID> expected = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < threads; i++) {
            pool.execute(() -> {
                for (int j = 0; j < perThread; j++) {
                    UUID id = UUID.randomUUID();
                    expected.add(id);
                    t.markPlayer(id);
                }
                done.countDown();
            });
        }
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        Set<UUID> drained = new HashSet<>(t.drain());
        assertEquals(expected, drained);
        assertEquals(Set.of(), t.drain());
    }
}
