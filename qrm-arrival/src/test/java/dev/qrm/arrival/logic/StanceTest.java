package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.arrival.logic.ArrivalSession.Phase;
import org.junit.jupiter.api.Test;

class StanceTest {
    @Test
    void lyingPhase_isLyingDown() {
        assertEquals(Stance.LYING, Stance.of(Phase.LYING, 10, 60, 40));
    }

    @Test
    void firstHalfOfRising_isCrouching() {
        assertEquals(Stance.CROUCHING, Stance.of(Phase.RISING, 60, 60, 40));
        assertEquals(Stance.CROUCHING, Stance.of(Phase.RISING, 79, 60, 40));
    }

    @Test
    void secondHalfOfRising_isStanding() {
        assertEquals(Stance.STANDING, Stance.of(Phase.RISING, 80, 60, 40));
    }

    @Test
    void doneAndAborted_areStanding() {
        assertEquals(Stance.STANDING, Stance.of(Phase.DONE, 100, 60, 40));
        assertEquals(Stance.STANDING, Stance.of(Phase.ABORTED, 5, 60, 40));
    }
}
