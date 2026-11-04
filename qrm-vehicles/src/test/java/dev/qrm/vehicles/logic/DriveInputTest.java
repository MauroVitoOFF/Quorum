package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Il contratto dell'input che arriva dalla rete: qualunque cosa mandi il client, il server vede valori ammessi. */
class DriveInputTest {

    @Test
    void valoriFuoriIntervalloVengonoPortatiAiLimiti() {
        DriveInput in = new DriveInput(5.0, -9.0, true).clamped();
        assertEquals(1.0, in.throttle());
        assertEquals(-1.0, in.steer());
        assertTrue(in.brake());
    }

    @Test
    void nanDiventaZero() {
        DriveInput in = new DriveInput(Double.NaN, Double.NaN, false).clamped();
        assertEquals(0.0, in.throttle());
        assertEquals(0.0, in.steer());
    }

    @Test
    void infinitoSiFermaAlLimite() {
        DriveInput in = new DriveInput(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, false).clamped();
        assertEquals(1.0, in.throttle());
        assertEquals(-1.0, in.steer());
    }

    @Test
    void valoriValidiPassanoInvariati() {
        DriveInput in = new DriveInput(0.4, 0.2, false).clamped();
        assertEquals(0.4, in.throttle());
        assertEquals(0.2, in.steer());
    }
}
