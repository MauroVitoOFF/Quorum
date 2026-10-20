package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class BallisticsTest {
    @Test
    void cinqueColpiAzzeranoVentiPuntiVita() {
        float hp = 20f;
        for (int i = 0; i < 5; i++) {
            hp -= Ballistics.damage(false);
        }
        assertEquals(0f, Math.max(0f, hp));
        float quattro = 20f - 4 * Ballistics.damage(false);
        assertTrue(quattro > 0f);
    }

    @Test
    void costantiDellaSpecifica() {
        assertEquals(6.0f, Ballistics.damage(true));
        assertEquals(3.0, Ballistics.spreadDegrees(false));
        assertEquals(0.5, Ballistics.spreadDegrees(true));
        assertEquals(64.0, Ballistics.RANGE);
    }

    private static double[] dir(double yawDeg, double pitchDeg) {
        double y = Math.toRadians(yawDeg);
        double p = Math.toRadians(pitchDeg);
        return new double[] {-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    @Test
    void deviazioneSempreEntroIlCono() {
        RandomGenerator rnd = new java.util.Random(42);
        for (boolean aiming : new boolean[] {false, true}) {
            double spread = Ballistics.spreadDegrees(aiming);
            for (int i = 0; i < 10_000; i++) {
                double yaw = rnd.nextDouble() * 360 - 180;
                double pitch = rnd.nextDouble() * 180 - 90;
                double[] r = Ballistics.deviate(yaw, pitch, aiming, rnd);
                double[] a = dir(yaw, pitch);
                double[] b = dir(r[0], r[1]);
                double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
                double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
                assertTrue(angle <= spread + 1e-6, "angolo " + angle + " > " + spread);
                assertTrue(r[1] >= -90 && r[1] <= 90);
            }
        }
    }
}
