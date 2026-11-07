package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SteerSmoothingTest {

    @Test
    void ilVolanteNonScattaAlTastoPremuto() {
        double s = SteerSmoothing.step(0, 1, 0);
        assertEquals(SteerSmoothing.RISE_SLOW, s, 1e-9);
        int ticks = 0;
        while (s < 1) {
            s = SteerSmoothing.step(s, 1, 0);
            ticks++;
        }
        assertTrue(ticks >= 6, "da fermo serve più di mezzo secondo scarso: " + ticks);
    }

    @Test
    void piuVeloceSiSterzaPiuPiano() {
        assertTrue(SteerSmoothing.step(0, 1, 1) < SteerSmoothing.step(0, 1, 0));
    }

    @Test
    void ilRitornoAlCentroEPiuVeloce() {
        assertEquals(0.5 - SteerSmoothing.RETURN, SteerSmoothing.step(0.5, 0, 0.5), 1e-9);
        // Invertire il verso passa per il ritorno, non per la salita lenta.
        assertEquals(0.5 - SteerSmoothing.RETURN, SteerSmoothing.step(0.5, -1, 0.5), 1e-9);
        assertEquals(0, SteerSmoothing.step(0.1, 0, 0), 1e-9);
    }
}
