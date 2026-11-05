package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class SeatMathTest {

    private static final double EPS = 1e-9;

    @Test
    void conIlMusoVersoPiuZLaSinistraDelModelloEPiuX() {
        // Il davanti del modello è -Z; con imbardata 0 il veicolo guarda verso +Z (sud), dove la sua sinistra è +X.
        double[] w = SeatMath.toWorld(-0.4, 0.2, 0);
        assertArrayEquals(new double[] {0.4, -0.2}, w, EPS); // dietro il centro = z negativo
    }

    @Test
    void ilDavantiDelModelloSegueIlMuso() {
        // Imbardata 90: il veicolo guarda verso -X; un punto davanti (mz = -1) cade a x = -1.
        double[] w = SeatMath.toWorld(0, -1, 90);
        assertArrayEquals(new double[] {-1, 0}, w, 1e-9);
    }

    @Test
    void ilCentroResta() {
        assertArrayEquals(new double[] {0, 0}, SeatMath.toWorld(0, 0, 123), EPS);
    }
}
