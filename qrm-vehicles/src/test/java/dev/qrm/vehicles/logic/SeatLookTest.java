package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeatLookTest {
    @Test
    void laTestaSegueLoSguardoRispettoAlCorpo() {
        assertEquals(30f, SeatLook.headRelativeYaw(130f, 100f), 1e-4f);
        assertEquals(-20f, SeatLook.headRelativeYaw(80f, 100f), 1e-4f);
    }

    @Test
    void ilGiroDi360GradiNonCreaSalti() {
        assertEquals(20f, SeatLook.headRelativeYaw(-170f, 170f), 1e-4f);
        assertEquals(-20f, SeatLook.headRelativeYaw(170f, -170f), 1e-4f);
    }

    @Test
    void ilColloNonGiraOltreIlLimite() {
        assertEquals(SeatLook.MAX_HEAD_TURN, SeatLook.headRelativeYaw(180f, 0f), 1e-4f);
        assertEquals(-SeatLook.MAX_HEAD_TURN, SeatLook.headRelativeYaw(-135f, 0f), 1e-4f);
    }
}
