package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.arrival.logic.ArrivalSession.Phase;
import org.junit.jupiter.api.Test;

class ArrivalSessionTest {
    private static void tick(ArrivalSession s, int n) {
        for (int i = 0; i < n; i++) s.tick();
    }

    @Test
    void startsInLying_locked() {
        ArrivalSession s = new ArrivalSession(3, 2, 100);
        assertEquals(Phase.LYING, s.phase());
        assertTrue(s.locksPlayer());
        assertFalse(s.completed());
    }

    @Test
    void afterLyingTicks_becomesRising() {
        ArrivalSession s = new ArrivalSession(3, 2, 100);
        tick(s, 2);
        assertEquals(Phase.LYING, s.phase());
        assertEquals(Phase.RISING, s.tick());
        assertTrue(s.locksPlayer());
    }

    @Test
    void afterLyingPlusRising_becomesDone_unlocked_completed() {
        ArrivalSession s = new ArrivalSession(3, 2, 100);
        tick(s, 4);
        assertEquals(Phase.RISING, s.phase());
        assertEquals(Phase.DONE, s.tick());
        assertFalse(s.locksPlayer());
        assertTrue(s.completed());
    }

    @Test
    void abort_neverCompletes_andUnlocks() {
        ArrivalSession s = new ArrivalSession(3, 2, 100);
        tick(s, 2);
        s.abort();
        assertEquals(Phase.ABORTED, s.phase());
        assertFalse(s.locksPlayer());
        tick(s, 10);
        assertEquals(Phase.ABORTED, s.phase());
        assertFalse(s.completed());
    }

    @Test
    void exceedingMaxLock_forcesDoneWithoutCompleted() {
        ArrivalSession s = new ArrivalSession(50, 50, 10);
        tick(s, 10);
        assertEquals(Phase.DONE, s.phase());
        assertFalse(s.locksPlayer());
        assertFalse(s.completed());
    }

    @Test
    void tickAfterDone_staysDone() {
        ArrivalSession s = new ArrivalSession(1, 1, 100);
        tick(s, 5);
        assertEquals(Phase.DONE, s.phase());
        assertTrue(s.completed());
        assertEquals(Phase.DONE, s.tick());
    }
}
