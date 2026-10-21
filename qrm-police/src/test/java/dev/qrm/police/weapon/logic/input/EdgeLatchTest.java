package dev.qrm.police.weapon.logic.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EdgeLatchTest {
    @Test
    void segnalaSoloICambi() {
        EdgeLatch l = new EdgeLatch();
        assertFalse(l.update(false));
        assertTrue(l.update(true));
        assertTrue(l.value());
        assertFalse(l.update(true));
        assertFalse(l.update(true));
        assertTrue(l.update(false));
        assertFalse(l.value());
        assertFalse(l.update(false));
    }

    @Test
    void resetTornaAFalsoSenzaSegnalare() {
        EdgeLatch l = new EdgeLatch();
        l.update(true);
        l.reset();
        assertFalse(l.value());
        assertTrue(l.update(true));
    }
}
