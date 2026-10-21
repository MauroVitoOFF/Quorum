package dev.qrm.police.weapon.logic.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KickDecayTest {
    private static final float EPS = 1e-5f;

    @Test
    void aRiposoNessunoSpostamento() {
        KickDecay k = new KickDecay();
        assertEquals(0f, k.pitch(0.5f), EPS);
        assertEquals(0f, k.yaw(0.5f), EPS);
    }

    @Test
    void ilContraccolpoArrivaSubitoERientraInSeiTick() {
        KickDecay k = new KickDecay();
        k.add(6f, 3f);
        // Subito, senza rampa di ingresso.
        assertEquals(6f, k.pitch(0f), EPS);
        assertEquals(6f, k.pitch(1f), EPS);
        for (int i = 1; i <= KickDecay.RETURN_TICKS; i++) {
            k.tick();
            float expected = 6f * (KickDecay.RETURN_TICKS - i) / KickDecay.RETURN_TICKS;
            assertEquals(expected, k.pitch(1f), EPS, "tick " + i);
            assertEquals(expected / 2f, k.yaw(1f), EPS, "tick " + i);
        }
        assertEquals(0f, k.pitch(1f), EPS);
        k.tick();
        assertEquals(0f, k.pitch(0f), EPS);
        assertEquals(0f, k.pitch(0.5f), EPS);
    }

    @Test
    void interpolaTraUnTickEIlSuccessivo() {
        KickDecay k = new KickDecay();
        k.add(6f, 0f);
        k.tick(); // 6 -> 5
        assertEquals(5.5f, k.pitch(0.5f), EPS);
        assertEquals(6f, k.pitch(0f), EPS);
        assertEquals(5f, k.pitch(1f), EPS);
    }

    @Test
    void colpiRavvicinatiSiSommanoERientranoInSeiTickDallUltimo() {
        KickDecay k = new KickDecay();
        k.add(6f, 0f);
        k.tick();
        k.tick(); // 4
        k.add(6f, 0f); // 10
        assertEquals(10f, k.pitch(1f), EPS);
        for (int i = 0; i < KickDecay.RETURN_TICKS; i++) k.tick();
        assertEquals(0f, k.pitch(1f), EPS);
    }

    @Test
    void resetAzzera() {
        KickDecay k = new KickDecay();
        k.add(3f, 1f);
        k.reset();
        assertEquals(0f, k.pitch(0.3f), EPS);
        assertEquals(0f, k.yaw(0.3f), EPS);
    }
}
