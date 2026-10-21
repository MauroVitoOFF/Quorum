package dev.qrm.police.weapon.logic.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InputRateLimitTest {
    @Test
    void dieciNelloStessoTickPoiBasta() {
        InputRateLimit limit = new InputRateLimit();
        for (int i = 0; i < 10; i++) assertTrue(limit.allow(100), "intenzione " + i);
        assertFalse(limit.allow(100));
        assertFalse(limit.allow(119));
        // La finestra scorre: dal tick 120 le dieci del tick 100 escono.
        for (int i = 0; i < 10; i++) assertTrue(limit.allow(120), "intenzione " + i);
        assertFalse(limit.allow(120));
    }

    @Test
    void lIgnorataNonOccupaPosto() {
        InputRateLimit limit = new InputRateLimit(2, 20);
        assertTrue(limit.allow(0));
        assertTrue(limit.allow(10));
        for (int t = 11; t < 20; t++) assertFalse(limit.allow(t));
        assertTrue(limit.allow(20));
        assertFalse(limit.allow(29));
        assertTrue(limit.allow(30));
    }

    @Test
    void unUsoNormaleNonVieneMaiLimitato() {
        InputRateLimit limit = new InputRateLimit();
        // Un'intenzione ogni 2 tick: 10 al secondo esatti.
        for (long t = 0; t < 2000; t += 2) assertTrue(limit.allow(t), "tick " + t);
    }

    @Test
    void parametriNonValidi() {
        assertThrows(IllegalArgumentException.class, () -> new InputRateLimit(0, 20));
        assertThrows(IllegalArgumentException.class, () -> new InputRateLimit(10, 0));
    }
}
