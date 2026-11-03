package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EyeEffectTest {
    @Test
    void veil_startsDark_endsClear() {
        assertEquals(1.0f, EyeEffect.veil(0, 100), 0.0001f);
        assertEquals(0.0f, EyeEffect.veil(100, 100), 0.0001f);
    }

    @Test
    void veil_neverGrows() {
        float prev = EyeEffect.veil(0, 100);
        for (int t = 1; t <= 100; t++) {
            float v = EyeEffect.veil(t, 100);
            assertTrue(v <= prev, "tick " + t);
            prev = v;
        }
    }

    @Test
    void veil_isClampedOutsideTheRange() {
        assertEquals(1.0f, EyeEffect.veil(-5, 100), 0.0001f);
        assertEquals(0.0f, EyeEffect.veil(500, 100), 0.0001f);
    }

    @Test
    void eyelids_areClosedAtStart_openAfterTheLyingPhase() {
        assertEquals(1.0f, EyeEffect.eyelids(0, 60), 0.0001f);
        assertEquals(0.0f, EyeEffect.eyelids(60, 60), 0.0001f);
        assertEquals(0.0f, EyeEffect.eyelids(200, 60), 0.0001f);
    }

    @Test
    void zeroDuration_doesNotDivideByZero() {
        assertEquals(0.0f, EyeEffect.veil(0, 0), 0.0001f);
        assertEquals(0.0f, EyeEffect.eyelids(0, 0), 0.0001f);
    }
}
